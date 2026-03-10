package org.goden.svdemo.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileOperationService {
    String uploadImage(MultipartFile file);
}
