package org.goden.svdemo.controller;

import org.goden.svdemo.pojo.Result;
import org.goden.svdemo.service.FileOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileOperationController {

    @Autowired
    private FileOperationService fileOperationService;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String s = fileOperationService.uploadImage(file);
        return Result.success(s);
    }
}