package org.goden.svdemo.service.impl;

import org.goden.svdemo.exception.BusinessException;
import org.goden.svdemo.mapper.UserMapper;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.service.PasswordService;
import org.goden.svdemo.service.UserService;
import org.goden.svdemo.utils.ThreadLocalUtil;
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
        if(user == null) throw new BusinessException("该用户不存在!");
        return user;
    }

    @Override
    public User findUserByUserNameAndPassword(String username, String password){
        String encodedPassword = passwordService.encodePassword(password);
        User user = userMapper.findUserByUserNameAndPassword(username,encodedPassword);
        if(user == null) throw new BusinessException("账号密码错误!");

        user.setPassword("");
        return user;
    }

    @Override
    public String login(User user) {

        User u = findUserByUserNameAndPassword(user.getUsername(), user.getPassword());

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", u.getId());
        claims.put("username", u.getUsername());
        //响应token
        return jwtService.getToken(claims);
    }

    @Override
    public void update(User user) {
        Map<String, Object> token = ThreadLocalUtil.get();
        Integer id = (Integer) token.get("id");
        if(id == null) throw new BusinessException("请重新登录!");
        user.setId(id);
        //仅更新nickname email userPic
        userMapper.updateById(user);
    }

    @Override
    public void updateAvatar(String avatarUrl) {

        if(avatarUrl == null || avatarUrl.isEmpty()){
            throw new BusinessException("头像不能为空!");
        }

        Map<String, Object> token = ThreadLocalUtil.get();
        Integer id = (Integer) token.get("id");
        User user = new User();
        user.setId(id);
        user.setUserPic(avatarUrl);
        userMapper.updateAvatarById(user);
    }

    @Override
    public void updatePassword(Map<String,String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        String rePassword = params.get("rePassword");

        if(!StringUtils.hasText(oldPassword)){
            throw new BusinessException("原密码不能为空!");
        }
        if(!StringUtils.hasText(newPassword)){
            throw new BusinessException("新密码不能为空!");
        }
        if(!StringUtils.hasText(rePassword)){
            throw new BusinessException("二次验证密码不能为空!");
        }

        Map<String, Object> token = ThreadLocalUtil.get();
        Integer id = (Integer) token.get("id");

        User user = userMapper.findUserById(id);
        String password = user.getPassword();
        if(!passwordService.encodePassword(oldPassword).equals(password)){
            throw new BusinessException("原密码错误!");
        }

        if(!(newPassword.length() >= 6 && newPassword.length() <= 16)){
            throw new BusinessException("密码长度必须在6-16个字符之间!");
        }
        if(!newPassword.matches("^(?=.*[0-9])(?=.*[a-zA-Z]).{6,16}$")){
            throw new BusinessException("密码必须包含至少一个字母和一个数字!");
        }
        if(!newPassword.equals(rePassword)){
            throw new BusinessException("重置密码和二次验证密码不一致!");
        }

        String s = passwordService.encodePassword(newPassword);
        user.setPassword(s);

        userMapper.updatePassword(user);
    }

    @Override
    public void register(User user) {
        User u = userMapper.findUserByUserName(user.getUsername());
        if(u != null) throw new BusinessException("该用户名已存在!");

        String password = user.getPassword();
        String s = passwordService.encodePassword(password);
        user.setPassword(s);
        userMapper.add(user);
    }
}
