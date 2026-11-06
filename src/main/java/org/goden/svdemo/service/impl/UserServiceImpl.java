package org.goden.svdemo.service.impl;

import org.goden.svdemo.exception.BusinessException;
import org.goden.svdemo.mapper.RoleMapper;
import org.goden.svdemo.mapper.UserMapper;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.service.PasswordService;
import org.goden.svdemo.service.UserService;
import org.goden.svdemo.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private RoleMapper roleMapper;


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

    @Override
    public Map<String, String> login(User user) {
        User u = findUserByUserNameAndPassword(user.getUsername(), user.getPassword());

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", u.getId());
        claims.put("username", u.getUsername());
        // 写入当前版本号；旋转时旧 token 的 tv 会原样继承
        claims.put("tv", jwtService.currentTokenVersion(u.getId()));

        // 生成双 token
        String accessToken = jwtService.generateAccessToken(claims);
        String refreshToken = jwtService.generateRefreshToken(claims);

        // 返回双 token（不登记；同一账号多端登录互不影响，仅改密时全端作废）
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    @Override
    @Transactional
    public void update(User user) {
        Long id = SecurityUtil.getCurrentUserId();
        if (id == null) throw new BusinessException("请重新登录!");
        user.setId(id);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
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

    // ========== 改密：版本号 +1，全端强制下线 ==========

    // 改密成功回调：立即清状态 + 跳登录页
//    fetch('/user/updatePassword', {
//        method: 'PATCH',
//                headers: {
//            'Authorization': 'Bearer ' + localStorage.getItem('accessToken'),
//                    'Content-Type': 'application/json'
//        },
//        body: JSON.stringify({ oldPassword, newPassword, rePassword })
//    }).then(res => res.json()).then(data => {
//        if (data.code === 200) {
//            localStorage.removeItem('accessToken');   // rt 是 HttpOnly，服务端已使其失效
//            window.location.href = '/login';
//        }
//    });

    // 全局响应拦截：token_invalid / refresh 失败(401) → 回登录页
    // axios 示例：
//    axios.interceptors.response.use(r => r, err => {
//            if (err.response && err.response.status === 401) {
//            const code = err.response.data?.code;
//                if (code === 'token_invalid' || code === 401) {
//                    // access 被判定失效（含改密作废），且 /user/refresh 也会 401
//                    localStorage.removeItem('accessToken');
//                    window.location.href = '/login';
//                } else if (code === 'access_token_expired') {
//                    // 正常过期才走静默刷新
//                    refreshToken();
//                }
//            }
//            return Promise.reject(err);
//        });
    @Override
    @Transactional
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

        // 改密即全端失效：版本号 +1，该用户所有旧 access/refresh 立即作废
        jwtService.invalidateUserTokens(id);

    }

    @Override
    @Transactional
    public void register(User user) {
        User u = userMapper.findUserByUserName(user.getUsername());
        if (u != null) throw new BusinessException("该用户名已存在!");

        String s = passwordService.encodePassword(user.getPassword());
        user.setPassword(s);
        userMapper.add(user);
        roleMapper.add(user);
    }
}