package org.goden.svdemo.vo;

import lombok.Data;

/** 登录/刷新响应体：只有 access token，refresh 只走 Set-Cookie */
@Data
public class LoginVO {

    private String accessToken;
    private String tokenType = "Bearer";

    public LoginVO() {}

    public LoginVO(String accessToken) {
        this.accessToken = accessToken;
    }
}