package org.goden.svdemo.controller;

import jakarta.validation.Valid;
import org.goden.svdemo.anno.ValidationGroups;
import org.goden.svdemo.pojo.Result;
import org.goden.svdemo.pojo.User;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.service.UserService;
import org.goden.svdemo.utils.ThreadLocalUtil;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
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
    public Result<Void> register(@Validated(ValidationGroups.Create.class) @RequestBody User user){

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
        if(user == null) return Result.error("该用户不存在!");
        return Result.success(user);
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> update(@Validated(ValidationGroups.Update.class) @RequestBody User user){
        userService.update(user);
        return Result.success();
    }

    @PatchMapping(value = "/updateAvatar", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> updateAvatar(@RequestParam @URL String avatarUrl){
        if(avatarUrl == null || avatarUrl.isEmpty()){
            return Result.error("头像不能为空!");
        }
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    @PatchMapping(value = "/updatePassWord", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> updatePassWord(@RequestBody Map<String, String> params){

        return Result.success();
    }
}
