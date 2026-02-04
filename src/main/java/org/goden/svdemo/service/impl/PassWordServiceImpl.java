package org.goden.svdemo.service.impl;

import org.goden.svdemo.service.PassWordService;
import org.springframework.stereotype.Service;

@Service
public class PassWordServiceImpl implements PassWordService {

    //加密
    @Override
    public String encodePassword(String rawPassword) {
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        return encoder.encode(rawPassword);
        return rawPassword;
    }

    //解密
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        return encoder.matches(rawPassword, encodedPassword);
        return true;
    }
}
