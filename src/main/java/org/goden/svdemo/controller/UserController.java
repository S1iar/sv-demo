package org.goden.svdemo.controller;

import org.goden.svdemo.anno.ValidationGroups;
import org.goden.svdemo.entity.Result;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.service.UserService;
import org.goden.svdemo.support.AuthCookieHelper;
import org.goden.svdemo.utils.SecurityUtil;
import org.goden.svdemo.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private AuthCookieHelper authCookieHelper;

    // 管理员权限(角色权限)
    // @PreAuthorize("hasRole('ADMIN')")

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> register(@Validated(ValidationGroups.Create.class) @RequestBody User user) {
        userService.register(user);
        return Result.success("注册成功!");
    }

    // 以下是前端操作
    // 登录成功：
    // localStorage.setItem('accessToken', res.accessToken);
    // refreshToken 只通过 HttpOnly Cookie 下发（JS 读不到，不需要也不能存 localStorage）
    // 改密成功回调
    // localStorage.clear();
    // window.location.href = '/login';
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<LoginVO>> login(@Validated(ValidationGroups.Login.class) @RequestBody User user) {
        Map<String, String> tokens = userService.login(user);
        ResponseCookie rt = authCookieHelper.build(tokens.get("refreshToken"));
        LoginVO vo = new LoginVO(tokens.get("accessToken"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rt.toString())
                .body(Result.success(vo));
    }

    /**
     * 刷新：只从 HttpOnly cookie 读取 rt，杜绝 refresh token 经由请求体/前端存储泄露。
     * 旋转后同时下发新的 access 和新的 rt cookie。
     */
    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<LoginVO>> refresh(
            @CookieValue(name = AuthCookieHelper.RT_NAME, required = false) String rtCookie) {

        Map<String, String> tokens = jwtService.rotateRefreshToken(rtCookie);

        ResponseCookie newRt = authCookieHelper.build(tokens.get("refreshToken"));
        LoginVO vo = new LoginVO(tokens.get("accessToken"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRt.toString())
                .body(Result.success(vo));
    }

//    前端登出（refresh token 由浏览器自动携带 HttpOnly Cookie，无需手动传）
//    fetch('/user/logout', {
//        method: 'POST',
//                headers: {
//            'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
//        }
//    }).then(() => {
//        localStorage.clear();
//        window.location.href = '/login';
//    });
    /**
     * 登出：拉黑 access（可能已过期，无所谓）+ 拉黑 refresh + 清 cookie。
     * 不强依赖 access 有效，因此去掉 user:out 权限；只要过 Filter（或 access 过期后
     * 把 logout 加入 shouldNotFilter 也可以）。
     */
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            @CookieValue(name = AuthCookieHelper.RT_NAME, required = false) String rtCookie) {

        jwtService.revokeAccessToken(accessToken);

        if (rtCookie != null) {
            jwtService.revokeRefreshToken(rtCookie);
        }

        ResponseCookie cleared = authCookieHelper.clear();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .body(Result.success("已退出登录!"));
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