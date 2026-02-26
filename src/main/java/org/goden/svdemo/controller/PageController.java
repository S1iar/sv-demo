package org.goden.svdemo.controller;

import org.goden.svdemo.pojo.Result;
import org.goden.svdemo.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/page")
public class PageController {

    @Autowired
    PageService pageService;

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<String>> list(){
        List<String> list = pageService.list();
        return Result.success(list);
    }

}
