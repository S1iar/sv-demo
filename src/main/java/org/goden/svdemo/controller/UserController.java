package org.goden.svdemo.controller;

import org.goden.svdemo.pojo.Result;
import org.goden.svdemo.pojo.User;
import org.goden.svdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping(value = "/register",produces = MediaType.APPLICATION_JSON_VALUE)
    public Result register(@RequestBody User user){

        if(userService.findUserByUserName(user.getUsername()) != null){
            return Result.error("该用户名已存在!");
        }

        userService.register(user);

        return Result.success();
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result login(@RequestBody User user){

        User u = userService.findUserByUserNameAndPassword(user);
        if(u == null) Result.error("登录失败：账号密码错误");

        String token = userService.login(u);


        return Result.success(token);
    }
}
