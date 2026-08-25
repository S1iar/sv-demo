package org.goden.svdemo.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.goden.svdemo.config.UploadConfig;
import org.goden.svdemo.entity.FileUploadResult;
import org.goden.svdemo.exception.BusinessException;
import org.goden.svdemo.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private UploadConfig uploadConfig;

    private final Tika tika = new Tika();

    /**
     * 上传文件
     * @param file 待上传的文件
     * @return 上传结果
     */
    public FileUploadResult uploadFile(MultipartFile file) {
        // 1. 基础判空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 2. 获取并净化文件名（防止路径穿越）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }
        originalFilename = StringUtils.cleanPath(originalFilename);

        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException("非法文件名");
        }

        // 3. 提取扩展名并转小写
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }
        if (extension.isEmpty()) {
            throw new BusinessException("文件缺少扩展名");
        }

        // 4. 白名单校验
        if (!uploadConfig.getAllowedExtensions().contains(extension)) {
            throw new BusinessException("不允许上传 ." + extension + " 类型的文件");
        }

        // 5. 根据扩展名获取文件大小限制
        long maxSize = uploadConfig.getExtensionMaxSize().getOrDefault(extension, uploadConfig.getDefaultMaxSize());
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制，最大允许 " + formatSize(maxSize));
        }

        // 6. 魔数校验（可选，增强安全性）
        validateMagicNumber(file, extension);

        // 7. 生成安全的存储文件名
        String safeFileName = UUID.randomUUID() + "." + extension;

        // 8. 构建目标路径
        Path uploadPath = Paths.get(uploadConfig.getUploadDir()).normalize();
        Path targetPath = uploadPath.resolve(safeFileName).normalize();

        // 9. 确保目录存在
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            log.error("创建上传目录失败", e);
            throw new BusinessException("服务器内部错误");
        }

        // 10. 写入文件
        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("文件写入失败: {}", safeFileName, e);
            throw new BusinessException("文件上传失败");
        }

        // 11. 返回结果
        String absolutePath = targetPath + safeFileName;
        return new FileUploadResult(safeFileName, absolutePath, file.getSize());
    }

    /**
     * 使用 Apache Tika 校验文件内容与扩展名是否匹配
     */
    private void validateMagicNumber(MultipartFile file, String extension) {
        // 获取期望的 MIME 类型
        String expectedMimeType = getExpectedMimeType(extension);
        if (expectedMimeType == null) {
            throw new BusinessException("不支持的文件扩展名: ." + extension);
        }

        try (InputStream is = file.getInputStream()) {
            // 使用 Tika 检测文件的实际 MIME 类型
            String detectedMimeType = tika.detect(is, file.getOriginalFilename());

            // 如果无法识别，拒绝上传
            if (detectedMimeType == null || "application/octet-stream".equals(detectedMimeType)) {
                throw new BusinessException("无法识别文件类型");
            }

            // 比较实际类型与期望类型
            if (!detectedMimeType.equals(expectedMimeType)) {
                log.warn("文件内容与扩展名不匹配: 检测到 {}，期望 {}", detectedMimeType, expectedMimeType);
                throw new BusinessException("文件内容与扩展名不匹配");
            }
        } catch (IOException e) {
            throw new BusinessException("读取文件内容失败");
        }
    }

    /**
     * 根据扩展名返回期望的 MIME 类型
     */
    private String getExpectedMimeType(String extension) {
        // 方式一：手动映射（推荐，清晰可控）
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"  -> "image/png";
            case "gif"  -> "image/gif";
            case "bmp"  -> "image/bmp";
            case "webp" -> "image/webp";
            case "pdf"  -> "application/pdf";
            case "doc"  -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls"  -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt"  -> "text/plain";
            case "zip"  -> "application/zip";
            case "rar"  -> "application/vnd.rar";
            default -> null;
        };
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
