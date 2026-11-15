package org.goden.svdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "download")
@Data
public class DownloadConfig {

    /** 单次导出行数（500） */
    private int exportRows = 500;

    /** 并行预取图片数（同时在内存中驻留的图片张数，峰值内存 ≈ prefetch × 5MB） */
    private int prefetch = 8;

    /** 单张图片大小上限，防止 urlPath 指向超大文件打爆内存 */
    private long maxImageSize = 20 * 1024 * 1024;

    /** 连接超时（秒） */
    private int connectTimeoutSeconds = 10;

    /** 读取超时（秒） */
    private int readTimeoutSeconds = 60;

    /** 是否允许 urlPath 指向内网地址：本地演示为 true，生产建议 false 启用 SSRF 拦截 */
    private boolean allowPrivateNetwork = true;
}