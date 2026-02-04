package org.goden.svdemo.controller;

import org.goden.svdemo.pojo.Result;
import org.goden.svdemo.pojo.User;
import org.goden.svdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result register(@RequestBody User user){

        if(userService.findUserByUserName(user.getUsername()) != null){
            return Result.error("该用户名已存在!");
        }

        userService.register(user);

        return Result.success();
    }

    @PostMapping("/login")
    public Result login(@RequestBody User user){

        String token = userService.login(user);

        return Result.success(token);
    }
}
