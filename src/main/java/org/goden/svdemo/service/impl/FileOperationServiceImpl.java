package org.goden.svdemo.service.impl;

import org.goden.svdemo.service.FileOperationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileOperationServiceImpl implements FileOperationService {

    // 定义图片存储目录（例如：D:/uploads/）
    private static final String UPLOAD_DIR = "D:/uploads/";

    @Override
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            return "文件为空";
        }

        try {
            // 确保上传目录存在
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名（避免重名覆盖）
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // 保存文件到本地
            Path filePath = Paths.get(UPLOAD_DIR + newFileName);
            Files.write(filePath, file.getBytes());

            return "文件上传成功: " + newFileName;
        } catch (IOException e) {
            e.printStackTrace();
            return "上传失败: " + e.getMessage();
        }
    }
}
