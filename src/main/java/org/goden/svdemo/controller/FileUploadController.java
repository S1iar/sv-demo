package org.goden.svdemo.controller;

import org.goden.svdemo.entity.FileUploadResult;
import org.goden.svdemo.entity.Result;
import org.goden.svdemo.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/file")
    public Result<FileUploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadResult result = fileUploadService.uploadFile(file);
        return Result.success(result);
    }
}
