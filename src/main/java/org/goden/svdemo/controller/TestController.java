package org.goden.svdemo.controller;

import org.goden.svdemo.pojo.test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    public test testDemo;
    @GetMapping("/yml")
    public String test() {
        List<Map<String, Object>> peoples = testDemo.getPeoples();
        Map<String, Object> stringObjectMap = peoples.get(0);
        String name = (String)stringObjectMap.get("name");
        Integer age = (Integer) stringObjectMap.get("age");
        return name+" "+age;
    }
}
