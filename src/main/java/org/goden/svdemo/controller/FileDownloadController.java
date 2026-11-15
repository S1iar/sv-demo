package org.goden.svdemo.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.goden.svdemo.service.FileDownloadService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/download")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileDownloadService fileDownloadService;

    @GetMapping("/quality-zip")
    public void exportQualityZip(HttpServletResponse response) throws IOException {
        String fileName = URLEncoder.encode("quality_export.zip", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=quality_export.zip; filename*=UTF-8''" + fileName);

        fileDownloadService.exportQualityZip(response.getOutputStream());
        response.flushBuffer();
    }
}