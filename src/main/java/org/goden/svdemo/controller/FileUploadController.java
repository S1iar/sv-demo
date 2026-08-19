package org.goden.svdemo.controller;

import org.goden.svdemo.entity.Result;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @PostMapping("/file")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        file.transferTo(new File("E:\\Project\\sv-demo\\src\\main\\resources\\uploadFile"+originalFilename));
        return Result.success("文件上传成功!");
    }
}
