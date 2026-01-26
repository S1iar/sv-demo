package org.goden.svdemo.controller;

import org.goden.svdemo.pojo.YmlTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/test")
public class TestController {
    @Qualifier("ymlTest")
    @Autowired
    public YmlTest ymlTestDemo;
    @GetMapping("/yml")
    public String test() {
        List<Map<String, Object>> peoples = ymlTestDemo.getPeoples();
        Map<String, Object> stringObjectMap = peoples.get(0);
        String name = (String)stringObjectMap.get("name");
        Integer age = (Integer) stringObjectMap.get("age");
        return name+" "+age;
    }

    @GetMapping("/math")
    public boolean mathTest() {
        String s1 = "sliar";
        String s2 = "sli"+"ar";

        return s1 == s2;
    }
}
