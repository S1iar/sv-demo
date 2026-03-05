package org.goden.svdemo.controller;

import jakarta.validation.Valid;
import org.goden.svdemo.pojo.Result;
import org.goden.svdemo.pojo.User;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.service.UserService;
import org.goden.svdemo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @PostMapping(value = "/register",produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> register(@Valid @RequestBody User user){

        if(userService.findUserByUserName(user.getUsername()) != null){
            return Result.error("该用户名已存在!");
        }

        userService.register(user);

        return Result.success();
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> login(@Valid @RequestBody User user){
        User u = userService.findUserByUserNameAndPassword(user);
        if(u == null) return Result.error("登录失败：账号密码错误");

        String token = userService.login(u);
        return Result.success(token);
    }

    @GetMapping(value = "/getUserInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<User> getUserInfoByToken(@RequestHeader(name = "Authorization") String token){
        Map<String, Object> map = jwtService.parseToken(token);
        String username = (String) map.get("username");
        User user = userService.findUserByUserName(username);
        return Result.success(user);
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> update(@RequestBody User user){
        //这里需要获取token中的用户
//        Object o = ThreadLocalUtil.get();
        userService.update(user);
        return Result.success();
    }
}
