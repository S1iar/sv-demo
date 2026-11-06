package org.goden.svdemo.service;

import com.auth0.jwt.exceptions.JWTVerificationException;
import java.util.Map;

public interface JwtService {

    /** 生成 access token（短时效，用于访问业务接口） */
    String generateAccessToken(Map<String, Object> claims);

    /** 生成 refresh token（长时效，仅用于刷新 access token） */
    String generateRefreshToken(Map<String, Object> claims);

    /**
     * 解析并校验 token：验签 + 类型校验 + 查黑名单 + 提取 user claim。
     * @param expectType "access" 或 "refresh"；传 null 表示不校验类型
     */
    Map<String, Object> parseToken(String token, String expectType);

    /** 便捷方法：Filter 只关心 access */
    default Map<String, Object> parseAccessToken(String token) {
        return parseToken(token, "access");
    }

    /**
     * 用 refresh token 换取新的 access + refresh（refresh 旋转）。
     * 每次刷新成功后旧 refresh jti 会被拉黑，做到用一次即废；
     * 支持 refresh-grace-period 宽限期。
     */
    Map<String, String> rotateRefreshToken(String refreshToken) throws JWTVerificationException;

    /** 将指定 jti 加入黑名单，TTL 为剩余有效期 */
    void revokeToken(String jti, long ttlSeconds);

    /** 直接传 token 字符串（可带 "Bearer " 前缀），内部解析 jti 和 TTL */
    void revokeAccessToken(String token);

    void revokeRefreshToken(String refreshToken);

    /** 检查指定 jti 是否已被吊销 */
    boolean isRevoked(String jti);

    /** 查询用户当前 token 版本号（无记录返回 0） */
    long currentTokenVersion(long userId);

    /** 版本号 +1：使该用户所有已签发的 access/refresh 立即全部失效（改密时调用） */
    void invalidateUserTokens(long userId);
}