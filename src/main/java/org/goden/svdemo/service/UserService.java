package org.goden.svdemo.service;

import org.goden.svdemo.entity.User;

import java.util.Map;

public interface UserService {
    User findUserByUserName(String username);

    User findUserByUserNameAndPassword(String username, String password);

    void register(User user);

    String login(User user);

    void update(User user);

    void updateAvatar(String avatarUrl);

    void updatePassword(Map<String,String> params);
}
