package org.goden.svdemo.service;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Map;

public interface JwtService {

    /** 生成 access token（短时效，用于访问业务接口） */
    String generateAccessToken(Map<String, Object> claims);

    /** 生成 refresh token（长时效，仅用于刷新 access token） */
    String generateRefreshToken(Map<String, Object> claims);

    /** 解析并校验 token：验签 + 查黑名单 + 提取 user claim */
    Map<String, Object> parseToken(String token);

    // 刷新（返回新的双 token）
    Map<String, String> refreshAccessToken(String refreshToken);

    /**
     * 用 refresh token 换取新的 access token（refresh 旋转）。
     * 每次刷新成功后，旧 refresh token 的 jti 会被加入黑名单，做到用一次即废。
     */
    Map<String, String> rotateRefreshToken(String refreshToken) throws JWTVerificationException;

    /** 将指定 jti 加入黑名单，TTL 为剩余有效期 */
    void revokeToken(String jti, long ttlSeconds);

    /** 直接传 token 字符串，内部自己解析 jti 和 TTL */
    void revokeAccessToken(String token);

    /** 检查指定 jti 是否已被吊销 */
    boolean isRevoked(String jti);

    void revokeRefreshToken(String refreshToken);
}
