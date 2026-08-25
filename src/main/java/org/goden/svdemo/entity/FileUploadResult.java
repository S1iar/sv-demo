package org.goden.svdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResult {
    private String fileName;      // 存储的文件名（UUID + 扩展名）
    private String absolutePath;  // 相对路径，如 /uploads/uuid.jpg
    private long size;            // 实际大小
}
