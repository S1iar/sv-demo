package org.goden.svdemo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.goden.svdemo.config.DownloadConfig;
import org.goden.svdemo.entity.Quality;
import org.goden.svdemo.exception.BusinessException;
import org.goden.svdemo.service.QualityService;
import org.goden.svdemo.service.FileDownloadService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileDownloadServiceImpl implements FileDownloadService {

    private static final String IMAGE_DIR = "images/";
    private static final String EXCEL_ENTRY_NAME = "quality_export.xlsx";
    private static final int[] PNG_SIGNATURE = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final QualityService qualityService;
    private final DownloadConfig downloadConfig;

    @Qualifier("imageFetchExecutor")
    private final Executor imageFetchExecutor;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            // 部分图片服务会 302 到 CDN，需跟随重定向（跨域场景常见）
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public void exportQualityZip(OutputStream outputStream) throws IOException {
        List<Quality> rows = loadRows();

        Path tempExcel = Files.createTempFile("quality_export", ".xlsx");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(outputStream, 64 * 1024))) {
            buildExcel(rows, tempExcel);
            writeDeflatedEntry(zos, EXCEL_ENTRY_NAME, tempExcel);
            writeImages(zos, rows);
            zos.finish();
        } finally {
            Files.deleteIfExists(tempExcel);
        }
    }

    private List<Quality> loadRows() {
        List<Quality> dbRows = qualityService.list();
        int limit = Math.min(dbRows.size(), downloadConfig.getExportRows());
        List<Quality> rows = new ArrayList<>(dbRows.subList(0, limit));
        // 演示兜底：数据不足 exportRows 时补占位行（urlPath 为空则生成无损占位 PNG）
        Random random = new Random();
        long demoId = 1;
        while (rows.size() < downloadConfig.getExportRows()) {
            Quality demo = new Quality();
            demo.setId(-demoId);
            demo.setValue1(random.nextInt(100));
            demo.setValue2(random.nextInt(100));
            demo.setInspectorId(demoId);
            demo.setUrlPath(null);
            rows.add(demo);
            demoId++;
        }
        return rows;
    }

    /**
     * SXSSF 流式写 Excel，滑动窗口 100 行，行数再多内存也恒定
     */
    private void buildExcel(List<Quality> rows, Path target) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            Sheet sheet = wb.createSheet("Quality 数据");
            CreationHelper helper = wb.getCreationHelper();

            Font linkFont = wb.createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            CellStyle linkStyle = wb.createCellStyle();
            linkStyle.setFont(linkFont);

            String[] headers = {"ID", "值1", "值2", "检验员ID", "检验图片", "原始图片URL"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < rows.size(); r++) {
                Quality q = rows.get(r);
                Row row = sheet.createRow(r + 1);
                row.createCell(0).setCellValue(q.getId());
                row.createCell(1).setCellValue(q.getValue1());
                row.createCell(2).setCellValue(q.getValue2());
                row.createCell(3).setCellValue(q.getInspectorId());

                Cell imageCell = row.createCell(4);
                String relativePath = IMAGE_DIR + imageFileName(q.getId());
                Hyperlink link = helper.createHyperlink(HyperlinkType.FILE);
                link.setAddress(relativePath);
                imageCell.setHyperlink(link);
                imageCell.setCellValue(relativePath);
                imageCell.setCellStyle(linkStyle);

                Cell urlCell = row.createCell(5);
                urlCell.setCellValue(q.getUrlPath() == null ? "-" : q.getUrlPath());
            }

            try (OutputStream os = Files.newOutputStream(target)) {
                wb.write(os);
            }
            wb.dispose();
        }
    }

    /**
     * 分批并行下载、按行序写入 zip：同时在途图片数 = prefetch，峰值内存 ≈ prefetch × 单图大小
     */
    private void writeImages(ZipOutputStream zos, List<Quality> rows) throws IOException {
        int prefetch = downloadConfig.getPrefetch();
        for (int i = 0; i < rows.size(); i += prefetch) {
            List<Quality> batch = rows.subList(i, Math.min(rows.size(), i + prefetch));
            List<CompletableFuture<byte[]>> futures = new ArrayList<>(batch.size());
            for (Quality q : batch) {
                futures.add(CompletableFuture.supplyAsync(() -> loadImage(q), imageFetchExecutor));
            }
            for (int j = 0; j < batch.size(); j++) {
                byte[] png;
                try {
                    png = futures.get(j).join();
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    throw new IOException("第 " + (i + j + 1) + " 行图片获取失败: " + cause.getMessage(), cause);
                }
                writeStoredEntry(zos, IMAGE_DIR + imageFileName(batch.get(j).getId()), png);
            }
        }
    }

    /**
     * 读取 urlPath 指向的图片。服务端 HTTP 请求不受浏览器 CORS 限制，可直连任意域。
     * 字节原样返回，不解码不重编码，保证无损。
     */
    private byte[] loadImage(Quality q) {
        if (q.getUrlPath() == null || q.getUrlPath().isBlank()) {
            return generatePlaceholderPng();
        }
        URI uri = validateUrl(q);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(downloadConfig.getReadTimeoutSeconds()))
                // 部分 CDN/图床会拒绝默认 Java UA 返回 403
                .header("User-Agent", "sv-demo-export/1.0")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new BusinessException("图片URL返回状态码 " + resp.statusCode() + ": " + uri);
            }
            byte[] body = resp.body();
            if (body.length > downloadConfig.getMaxImageSize()) {
                throw new BusinessException("图片超过大小限制: " + uri);
            }
            if (!isPng(body)) {
                throw new BusinessException("URL内容不是PNG图片: " + uri);
            }
            return body;
        } catch (IOException e) {
            throw new BusinessException("下载图片失败: " + uri + ", " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("下载图片被中断: " + uri);
        }
    }

    /**
     * SSRF 基础防护：仅允许 http/https，生产环境（allowPrivateNetwork=false）禁止指向内网地址
     */
    private URI validateUrl(Quality q) {
        URI uri;
        try {
            uri = URI.create(q.getUrlPath());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("非法的图片URL: " + q.getUrlPath());
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new BusinessException("图片URL协议不合法: " + q.getUrlPath());
        }
        if (!downloadConfig.isAllowPrivateNetwork()) {
            try {
                java.net.InetAddress addr = java.net.InetAddress.getByName(uri.getHost());
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                    throw new BusinessException("禁止访问内网图片地址: " + q.getUrlPath());
                }
            } catch (java.net.UnknownHostException e) {
                throw new BusinessException("图片域名无法解析: " + q.getUrlPath());
            }
        }
        return uri;
    }

    private boolean isPng(byte[] body) {
        if (body.length < 8) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if ((body[i] & 0xFF) != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    /** zip 中不可压缩的原始图片用 STORED：字节零改动、CPU 零浪费 */
    private void writeStoredEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    /** xlsx 本身是 zip 容器、压缩率低，用 DEFLATED 即可 */
    private void writeDeflatedEntry(ZipOutputStream zos, String name, Path file) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
    }

    /** 演示占位图：~5MB 无损 PNG，仅用于 urlPath 为空的兜底 */
    private byte[] generatePlaceholderPng() {
        try {
            int edge = 1280;
            BufferedImage image = new BufferedImage(edge, edge, BufferedImage.TYPE_3BYTE_BGR);
            byte[] pixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
            new Random().nextBytes(pixels);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(pixels.length + 4096);
            javax.imageio.ImageIO.write(image, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("生成占位图片失败");
        }
    }

    private String imageFileName(Long id) {
        return "quality_" + id + ".png";
    }
}