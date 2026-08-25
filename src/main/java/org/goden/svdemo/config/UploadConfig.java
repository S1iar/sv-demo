package org.goden.svdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "upload")
@Data
public class UploadConfig {

    /** 允许上传的扩展名列表（不含点） */
    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx", "txt");

    /** 默认最大文件大小（字节），例如 10MB */
    private long defaultMaxSize = 10 * 1024 * 1024L;

    /** 按扩展名定制的最大大小（字节），key 为扩展名（不含点） */
    private Map<String, Long> extensionMaxSize = new HashMap<>();

    /** 上传根目录 */
    private String uploadDir = "./uploads";
}
