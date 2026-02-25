package org.goden.svdemo.config;

import org.goden.svdemo.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //除了注册和登录 都要经过token认证的拦截
        registry.addInterceptor(loginInterceptor).excludePathPatterns(
                "/user/login",
                "/user/register"
        );
    }
}
