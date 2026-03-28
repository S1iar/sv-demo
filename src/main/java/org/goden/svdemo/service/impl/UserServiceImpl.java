package org.goden.svdemo.service.impl;

import org.goden.svdemo.exception.BusinessException;
import org.goden.svdemo.mapper.UserMapper;
import org.goden.svdemo.pojo.User;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.service.PassWordService;
import org.goden.svdemo.service.UserService;
import org.goden.svdemo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private PassWordService passWordService;

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
    public User findUserByUserNameAndPassword(User user){
        User u = userMapper.findUserByUserName(user.getUsername());
        if(u != null) {
            String s = passWordService.encodePassword(user.getPassword());
            if(u.getPassword().equals(s)){
                u.setPassword("");
                return u;
            }
        }
        return null;
    }

    @Override
    public String login(User user) {

        User u = findUserByUserNameAndPassword(user);
        if(u == null) throw new BusinessException("登录失败：账号密码错误!");

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
        user.setId(id);
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
    public void updatePassWord(Map<String,String> params) {
        String oldPassWord = params.get("oldPassWord");
        String newPassWord = params.get("newPassWord");
        String rePassWord = params.get("rePassWord");

        if(oldPassWord.isEmpty()){
            throw new BusinessException("旧密码不能为空!");
        }
        if(newPassWord.isEmpty()){
            throw new BusinessException("新密码不能为空!");
        }
        if(rePassWord.isEmpty()){
            throw new BusinessException("二次验证密码不能为空!");
        }

        Map<String, Object> token = ThreadLocalUtil.get();
        Integer id = (Integer) token.get("id");

        User user = userMapper.findUserById(id);
        String password = user.getPassword();
        if(!passWordService.encodePassword(password).equals(oldPassWord)){
            throw new BusinessException("旧密码错误!");
        }

        if(!(newPassWord.length() >= 6 && newPassWord.length() <= 16)){
            throw new BusinessException("密码长度必须在6-16个字符之间!");
        }
        if(!newPassWord.matches("^(?=.*[0-9])(?=.*[a-zA-Z]).{6,16}$")){
            throw new BusinessException("密码必须包含至少一个字母和一个数字!");
        }

        if(!newPassWord.equals(rePassWord)){
            throw new BusinessException("重置密码和二次验证密码不一致!");
        }

        user.setPassword(newPassWord);

        userMapper.updatePassWord(user);
    }

    @Override
    public void register(User user) {
        if(findUserByUserName(user.getUsername()) != null) throw new BusinessException("该用户名已存在!");

        String password = user.getPassword();
        String s = passWordService.encodePassword(password);
        user.setPassword(s);
        userMapper.add(user);
    }
}
