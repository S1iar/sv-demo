package org.goden.svdemo.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.goden.svdemo.mapper.UserMapper;
import org.goden.svdemo.pojo.Result;
import org.goden.svdemo.pojo.User;
import org.goden.svdemo.service.PassWordService;
import org.goden.svdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private PassWordService passWordService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findUserByUserName(String username) {
        User u = userMapper.findUserByUserName(username);
        return u;
    }

    @Override
    public User findUserByUserNameAndPassword(User user){
        User u = userMapper.findUserByUserName(user.getUsername());
        String s = passWordService.encodePassword(user.getPassword());
        if(u.getPassword().equals(s)){
            u.setPassword("");
            return u;
        }
        return null;
    }

    @Override
    public String login(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());

        String token  = JWT.create()
                .withClaim("user", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 30))
                .sign(Algorithm.HMAC256("SvDemoTest"));//密钥

        return token;
    }

    @Override
    public void register(User user) {
        String password = user.getPassword();
        String s = passWordService.encodePassword(password);

        System.out.println("输出:"+s.length());
        user.setPassword(s);
        userMapper.add(user);
    }
}
