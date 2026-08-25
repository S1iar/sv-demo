package org.goden.svdemo.service;

import org.goden.svdemo.entity.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    FileUploadResult uploadFile(MultipartFile file);
}
