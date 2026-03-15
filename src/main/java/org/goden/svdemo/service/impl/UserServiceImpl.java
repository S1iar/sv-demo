package org.goden.svdemo.service.impl;

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
        return userMapper.findUserByUserName(username);
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
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
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
        Map<String, Object> token = ThreadLocalUtil.get();
        Integer id = (Integer) token.get("id");
        User user = new User();
        user.setId(id);
        user.setUserPic(avatarUrl);
        userMapper.updateAvatarById(user);
    }

    @Override
    public void updatePassWord(User user) {
        Map<String, Object> token = ThreadLocalUtil.get();
        Integer id = (Integer) token.get("id");
        user.setId(id);
        userMapper.updatePassWord(user);
    }

    @Override
    public void register(User user) {
        String password = user.getPassword();
        String s = passWordService.encodePassword(password);
        user.setPassword(s);
        userMapper.add(user);
    }
}
