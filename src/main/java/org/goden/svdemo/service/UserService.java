package org.goden.svdemo.service;

import org.goden.svdemo.pojo.Result;
import org.goden.svdemo.pojo.User;

public interface UserService {
    User findUserByUserName(String username);

    void register(User user);

    String login(User user);
}
