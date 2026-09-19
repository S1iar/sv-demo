package org.goden.svdemo.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.goden.svdemo.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    /** access token 有效期（毫秒），默认 15 分钟 */
    @Value("${jwt.access-token-validity:900000}")
    private long accessTokenValidity;

    /** refresh token 有效期（毫秒），默认 7 天 */
    @Value("${jwt.refresh-token-validity:604800000}")
    private long refreshTokenValidity;

    /** 过期后的宽限期（毫秒），默认 15 分钟 */
    @Value("${jwt.refresh-grace-period:900000}")
    private long refreshGracePeriod;

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "revoked:";

    public JwtServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== Token 生成 ====================

    @Override
    public String generateAccessToken(Map<String, Object> claims) {
        String jti = UUID.randomUUID().toString();
        return JWT.create()
                .withClaim("user", claims)
                .withClaim("type", "access")
                .withJWTId(jti)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenValidity))
                .sign(Algorithm.HMAC256(secretKey));
    }

    @Override
    public String generateRefreshToken(Map<String, Object> claims) {
        String jti = UUID.randomUUID().toString();
        return JWT.create()
                .withClaim("user", claims)
                .withClaim("type", "refresh")
                .withJWTId(jti)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .sign(Algorithm.HMAC256(secretKey));
    }

    // ==================== Token 解析与校验 ====================

    @Override
    public Map<String, Object> parseToken(String token) {
        // 1. 验签名 + 验过期
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(secretKey))
                .build()
                .verify(token);

        // 2. 查黑名单（被吊销的 token 直接拒绝）
        String jti = decoded.getId();
        if (jti != null && isRevoked(jti)) {
            throw new JWTVerificationException("Token已失效，请重新登录");
        }

        // 3. 提取 user claim
        Map<String, Object> user = decoded.getClaim("user").asMap();
        if (user == null) {
            throw new JWTVerificationException("Token验证失败");
        }
        return user;
    }

    @Override
    public Map<String, String> refreshAccessToken(String refreshToken) throws JWTVerificationException {
        try {
            // 1. 验签
            DecodedJWT decoded = JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(refreshToken);

            // 2. 校验类型
            if (!"refresh".equals(decoded.getClaim("type").asString())) {
                throw new JWTVerificationException("Token类型错误");
            }

            // 3. 查黑名单
            String oldJti = decoded.getId();
            if (oldJti != null && isRevoked(oldJti)) {
                throw new JWTVerificationException("Refresh Token已失效，请重新登录");
            }

            // 4. 提取 claims
            Map<String, Object> claims = decoded.getClaim("user").asMap();

            // 5. 生成新双 token
            String newAccessToken = generateAccessToken(claims);
            String newRefreshToken = generateRefreshToken(claims);

            // 6. 旧 refresh 拉黑
            Date expiresAt = decoded.getExpiresAt();
            if (expiresAt != null) {
                long ttl = (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
                if (ttl > 0) revokeToken(oldJti, ttl);
            }

            // 7. 返回
            Map<String, String> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("refreshToken", newRefreshToken);
            return result;

        } catch (TokenExpiredException e) {
            throw new JWTVerificationException("Refresh Token已过期，请重新登录");
        }
    }

    // ==================== Refresh 旋转 ====================

    @Override
    public Map<String, String> rotateRefreshToken(String refreshToken) throws JWTVerificationException {
        // 1. 必须是 refresh 类型
        DecodedJWT decoded = JWT.decode(refreshToken);
        if (!"refresh".equals(decoded.getClaim("type").asString())) {
            throw new JWTVerificationException("无效的refresh token");
        }

        String oldJti = decoded.getId();
        Date oldExpiresAt = decoded.getExpiresAt();

        try {
            // 2. 正常未过期：验签 + 查黑名单
            Map<String, Object> claims = parseToken(refreshToken);

            // 3. 签发新的一对 token
            Map<String, String> newTokens = generateTokenPair(claims);

            // 4. 把旧 refresh jti 拉黑（用一次即废）
            blacklistToken(oldJti, oldExpiresAt);

            return newTokens;

        } catch (TokenExpiredException e) {
            // 5. 已过期但在宽限期内：同样允许刷新
            if (oldExpiresAt != null
                    && System.currentTimeMillis() - oldExpiresAt.getTime() <= refreshGracePeriod) {
                Map<String, Object> claims = decoded.getClaim("user").asMap();
                Map<String, String> newTokens = generateTokenPair(claims);
                blacklistToken(oldJti, oldExpiresAt);
                return newTokens;
            }
            throw e; // 超出宽限期，拒绝
        }
    }

    /** 同时签发新的 access + refresh（refresh 旋转核心） */
    private Map<String, String> generateTokenPair(Map<String, Object> claims) {
        Map<String, String> pair = new HashMap<>();
        pair.put("accessToken", generateAccessToken(claims));
        pair.put("refreshToken", generateRefreshToken(claims));
        return pair;
    }

    // ==================== 黑名单操作 ====================

    /** 将 token 加入黑名单，TTL = 剩余有效期 */
    @Override
    public void revokeToken(String jti, long ttlSeconds) {
        if (jti != null && ttlSeconds > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "1",
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        }
    }

    // 吊销 access token（Controller 调用）
    @Override
    public void revokeAccessToken(String token) {
        if (token == null || token.isEmpty()) return;
        try {
            // 兼容 "Bearer xxx" 和裸 token
            if (token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }
            DecodedJWT decoded = JWT.decode(token);
            String jti = decoded.getId();
            Date expiresAt = decoded.getExpiresAt();
            if (jti != null && expiresAt != null) {
                long ttlSeconds = (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
                revokeToken(jti, ttlSeconds);
            }
        } catch (Exception ignored) {
            // token 解析失败（格式不对/被篡改），直接忽略，登出照样成功
        }
    }

    // 吊销 refresh token（Controller 调用）
    @Override
    public void revokeRefreshToken(String refreshToken) {
        try {
            DecodedJWT decoded = JWT.decode(refreshToken);
            String jti = decoded.getId();
            Date expiresAt = decoded.getExpiresAt();
            if (jti != null && expiresAt != null) {
                long ttlSeconds = (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
                revokeToken(jti, ttlSeconds); // 复用底层方法，同一个黑名单
            }
        } catch (Exception ignored) {
            //解析失败,忽略
        }
    }

    /** 判断是否黑名单 */
    @Override
    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    /** 将 token 加入黑名单，TTL = 剩余有效期 */
    private void blacklistToken(String jti, Date expiresAt) {
        if (jti == null || expiresAt == null) return;
        long ttlSeconds = (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
        revokeToken(jti, ttlSeconds);
    }
}
