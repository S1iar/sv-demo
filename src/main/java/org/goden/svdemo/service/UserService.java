package org.goden.svdemo.service;

import org.goden.svdemo.pojo.User;

import java.util.Map;

public interface UserService {
    User findUserByUserName(String username);

    User findUserByUserNameAndPassword(String username, String password);

    void register(User user);

    String login(User user);

    void update(User user);

    void updateAvatar(String avatarUrl);

    void updatePassWord(Map<String,String> params);
}
