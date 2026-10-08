package org.goden.svdemo.controller;

import org.goden.svdemo.anno.ValidationGroups;
import org.goden.svdemo.entity.Result;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.service.UserService;
import org.goden.svdemo.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // 管理员权限(角色权限)
    // @PreAuthorize("hasRole('ADMIN')")

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> register(@Validated(ValidationGroups.Create.class) @RequestBody User user) {
        userService.register(user);
        return Result.success("注册成功!");
    }

    // 以下是前端操作
    // 登录成功
    // localStorage.setItem('accessToken', res.accessToken);
    // refreshToken 建议存 httpOnly cookie（如果前后端同域）,
    // 或者至少存 localStorage 但不要随便泄露
    //localStorage.setItem('refreshToken', res.refreshToken);
    // 改密成功回调
    // localStorage.clear();
    // window.location.href = '/login';
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, String>> login(@Validated(ValidationGroups.Login.class) @RequestBody User user) {
        Map<String, String> tokens = userService.login(user);
        return Result.success(tokens);
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        Map<String, String> tokens = jwtService.refreshAccessToken(refreshToken);
        return Result.success(tokens);
    }
//    前端登出
//    fetch('/user/logout', {
//        method: 'POST',
//                headers: {
//            'Authorization': 'Bearer ' + localStorage.getItem('accessToken'),
//                    'Content-Type': 'application/json'
//        },
//        body: JSON.stringify({
//                refreshToken: localStorage.getItem('refreshToken')
//    })
//    }).then(() => {
//        localStorage.clear();
//        window.location.href = '/login';
//    });
    // 用户功能权限(功能权限)
    @PreAuthorize("hasAuthority('user:out')")
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> logout(@RequestHeader("Authorization") String accessToken,
                                 @RequestBody(required = false) Map<String, String> body) {
        // 吊销 access token
        jwtService.revokeAccessToken(accessToken);

        // 吊销 refresh token（如果前端传了）
        if (body != null && body.containsKey("refreshToken")) {
            jwtService.revokeRefreshToken(body.get("refreshToken"));
        }

        return Result.success("已退出登录!");
    }

    // ==================== 用户资料相关（需要登录） ====================
    @PreAuthorize("hasAuthority('user:info')")
    @GetMapping(value = "/getUserInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<User> getUserInfo() {
        String userName = SecurityUtil.getCurrentUsername();
        if (userName == null) return Result.error(401, "请重新登录!");
        User user = userService.findUserByUserName(userName);
        return Result.success(user);
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PatchMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> update(@Validated(ValidationGroups.Update.class) @RequestBody User user) {
        userService.update(user);
        return Result.success("更新成功!");
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PatchMapping(value = "/updateAvatar", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> updateAvatar(@RequestParam String avatarUrl) {
        userService.updateAvatar(avatarUrl);
        return Result.success("头像已更新!");
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PatchMapping(value = "/updatePassword", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> updatePassword(@RequestBody Map<String, String> params) {
        userService.updatePassword(params);
        return Result.success("密码更新成功!");
    }
}