package org.goden.svdemo.service.impl;

import org.goden.svdemo.exception.BusinessException;
import org.goden.svdemo.mapper.UserMapper;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.service.PasswordService;
import org.goden.svdemo.service.UserService;
import org.goden.svdemo.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtService jwtService;

    @Override
    public User findUserByUserName(String username) {
        User user = userMapper.findUserByUserName(username);
        if (user == null) throw new BusinessException("该用户不存在!");
        return user;
    }

    @Override
    public User findUserByUserNameAndPassword(String username, String password) {
        String encodedPassword = passwordService.encodePassword(password);
        User user = userMapper.findUserByUserNameAndPassword(username, encodedPassword);
        if (user == null) throw new BusinessException("账号密码错误!");
        user.setPassword("");
        return user;
    }

    // ========== 登录：返回双 token，无互踢 ==========

    @Override
    public Map<String, String> login(User user) {
        User u = findUserByUserNameAndPassword(user.getUsername(), user.getPassword());

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", u.getId());
        claims.put("username", u.getUsername());

        // 生成双 token
        String accessToken = jwtService.generateAccessToken(claims);
        String refreshToken = jwtService.generateRefreshToken(claims);

        // 返回双 token（不登记,多端不互踢）
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    @Override
    public void update(User user) {
        Long id = SecurityUtil.getCurrentUserId();
        if (id == null) throw new BusinessException("请重新登录!");
        user.setId(id);
        userMapper.updateById(user);
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            throw new BusinessException("头像不能为空!");
        }
        Long id = SecurityUtil.getCurrentUserId();
        if (id == null) throw new BusinessException("请重新登录!");
        User user = new User();
        user.setId(id);
        user.setUserPic(avatarUrl);
        userMapper.updateAvatarById(user);
    }

    // ========== 改密：不再服务端强踢 ==========

    @Override
    public void updatePassword(Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        String rePassword = params.get("rePassword");

        if (!StringUtils.hasText(oldPassword)) throw new BusinessException("原密码不能为空!");
        if (!StringUtils.hasText(newPassword)) throw new BusinessException("新密码不能为空!");
        if (!StringUtils.hasText(rePassword)) throw new BusinessException("二次验证密码不能为空!");

        Long id = SecurityUtil.getCurrentUserId();
        if (id == null) throw new BusinessException("请重新登录!");

        User user = userMapper.findUserById(id);
        if (!passwordService.encodePassword(oldPassword).equals(user.getPassword())) {
            throw new BusinessException("原密码错误!");
        }
        if (!(newPassword.length() >= 6 && newPassword.length() <= 16)) {
            throw new BusinessException("密码长度必须在6-16个字符之间!");
        }
        if (!newPassword.matches("^(?=.*[0-9])(?=.*[a-zA-Z]).{6,16}$")) {
            throw new BusinessException("密码必须包含至少一个字母和一个数字!");
        }
        if (!newPassword.equals(rePassword)) {
            throw new BusinessException("重置密码和二次验证密码不一致!");
        }

        // 更新密码
        user.setPassword(passwordService.encodePassword(newPassword));
        userMapper.updatePassword(user);

        // 不再调用 clearRefreshToken
        // 前端收到改密成功响应后，主动清除本地 token 并跳登录页即可
    }

    @Override
    public void register(User user) {
        User u = userMapper.findUserByUserName(user.getUsername());
        if (u != null) throw new BusinessException("该用户名已存在!");

        String s = passwordService.encodePassword(user.getPassword());
        user.setPassword(s);
        userMapper.add(user);
    }
}