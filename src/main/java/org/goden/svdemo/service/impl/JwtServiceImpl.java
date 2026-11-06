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
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity:900000}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity:604800000}")
    private long refreshTokenValidity;

    @Value("${jwt.refresh-grace-period:900000}")
    private long refreshGracePeriod;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 统一黑名单前缀，避免 Redis key 污染
     */
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /** per-user token 版本号，改密后自增使旧 token 全量作废 */
    private static final String TOKEN_VERSION_PREFIX = "jwt:tv:";

    private Algorithm algorithm;

    public JwtServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(secretKey);
    }

    // ==================== Token 生成 ====================

    @Override
    public String generateAccessToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("user", claims)
                .withClaim("type", "access")
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenValidity))
                .sign(algorithm);
    }

    @Override
    public String generateRefreshToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("user", claims)
                .withClaim("type", "refresh")
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .sign(algorithm);
    }

    // ==================== Token 解析与校验 ====================

    @Override
    public Map<String, Object> parseToken(String token, String expectType) {
        DecodedJWT decoded = JWT.require(algorithm).build().verify(token);

        // 类型校验：refresh 不能当 access 用，反之亦然
        if (expectType != null) {
            String type = decoded.getClaim("type").asString();
            if (!expectType.equals(type)) {
                throw new JWTVerificationException("Token类型错误");
            }
        }

        String jti = decoded.getId();
        if (jti != null && isRevoked(jti)) {
            throw new JWTVerificationException("Token已失效，请重新登录");
        }

        Map<String, Object> user = decoded.getClaim("user").asMap();
        if (user == null) {
            throw new JWTVerificationException("Token验证失败");
        }

        // 版本号校验：改密后旧 access 立即失效
        Object userId = user.get("id");
        if (userId != null && currentTokenVersion(((Number) userId).longValue()) != tokenVersionOf(user)) {
            throw new JWTVerificationException("登录状态已失效，请重新登录!");
        }
        return user;
    }

    // ==================== Refresh 旋转 ====================

    @Override
    public Map<String, String> rotateRefreshToken(String refreshToken) throws JWTVerificationException {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new JWTVerificationException("缺少refresh token");
        }

        // 1. 先解出基本信息（不验签，仅取元数据）
        DecodedJWT decoded;
        try {
            decoded = JWT.decode(refreshToken);
        } catch (Exception e) {
            throw new JWTVerificationException("无效的refresh token");
        }
        if (!"refresh".equals(decoded.getClaim("type").asString())) {
            throw new JWTVerificationException("Token类型错误");
        }
        String oldJti = decoded.getId();
        Date oldExpiresAt = decoded.getExpiresAt();
        Map<String, Object> claims = decoded.getClaim("user").asMap();

        // fail-closed：合法 refresh 一定带 jti 和 exp，缺失则拒绝，防止绕过黑名单无限旋转
        if (oldJti == null || oldJti.isBlank() || oldExpiresAt == null) {
            throw new JWTVerificationException("无效的refresh token");
        }

        // 2. 先验签：防止伪造/篡改的串污染黑名单
        try {
            JWT.require(algorithm).build().verify(refreshToken);
        } catch (TokenExpiredException e) {
            // 已过期：仅宽限期内、且 claims 完整才允许
            boolean inGrace = System.currentTimeMillis() - oldExpiresAt.getTime() <= refreshGracePeriod;
            if (!inGrace || claims == null) {
                throw new JWTVerificationException("Refresh Token已过期，请重新登录");
            }
        } catch (JWTVerificationException e) {
            throw new JWTVerificationException("Token验证失败");
        }

        if (claims == null) {
            throw new JWTVerificationException("Token验证失败");
        }

        // 版本号校验：改密后旧 refresh 不允许再旋转（在抢占黑名单前检查，避免误烧 jti）
        Object uid = claims.get("id");
        if (uid != null && currentTokenVersion(((Number) uid).longValue()) != tokenVersionOf(claims)) {
            throw new JWTVerificationException("登录状态已失效，请重新登录!");
        }

        // 3. 抢占黑名单（SETNX）：验签通过后才写，解决并发重放
        //    未过期用剩余寿命做 TTL；已过期走宽限期的用 gracePeriod 兜底，防重复刷新
        long ttl = claimBlacklistTtl(oldExpiresAt);
        if (ttl <= 0) {
            ttl = refreshGracePeriod / 1000;
        }
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(BLACKLIST_PREFIX + oldJti, "1", ttl, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            // 已被别人（或自己上一次）占用 → 视为重放
            throw new JWTVerificationException("Refresh Token已失效，请重新登录");
        }

        // 4. 签发新的一对
        Map<String, String> pair = new HashMap<>();
        pair.put("accessToken", generateAccessToken(claims));
        pair.put("refreshToken", generateRefreshToken(claims));
        return pair;
    }

    /**
     * 计算剩余有效期（秒），已过期或空返回 <=0
     */
    private long claimBlacklistTtl(Date expiresAt) {
        if (expiresAt == null) return -1;
        return (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
    }

    // ==================== 黑名单操作 ====================

    @Override
    public void revokeToken(String jti, long ttlSeconds) {
        if (jti != null && ttlSeconds > 0) {
            stringRedisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void revokeAccessToken(String token) {
        revokeByPrefix(token, "Bearer ");
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        // refresh 只走 Cookie，是裸串；保留 decode 不验签以兼容已过期的登出场景
        revokeByPrefix(refreshToken, null);
    }

    /**
     * 统一处理：可带前缀、可带裸串；不验签，只用 decode
     */
    private void revokeByPrefix(String token, String prefix) {
        if (token == null || token.isEmpty()) return;
        if (prefix != null && token.startsWith(prefix)) {
            token = token.substring(prefix.length()).trim();
        }
        try {
            DecodedJWT decoded = JWT.decode(token);
            long ttl = claimBlacklistTtl(decoded.getExpiresAt());
            revokeToken(decoded.getId(), ttl);
        } catch (Exception ignored) {
            // 伪造/格式错，忽略，登出仍算成功
        }
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    @Override
    public long currentTokenVersion(long userId) {
        String key = TOKEN_VERSION_PREFIX + userId;
        String v = stringRedisTemplate.opsForValue().get(key);
        if (v == null) return 0L;
        // 滑动过期：只要该版本仍有 token 在被校验/签发，就续满一个 refresh 周期，
        // 防止 key 提前过期导致版本号回落 0，误杀临期前刚签发的合法 token
        stringRedisTemplate.expire(key, refreshTokenValidity, TimeUnit.MILLISECONDS);
        return Long.parseLong(v);
    }

    @Override
    public void invalidateUserTokens(long userId) {
        // 旧 token 最多还能存活一个 refresh 周期，TTL 覆盖它即可
        stringRedisTemplate.opsForValue().increment(TOKEN_VERSION_PREFIX + userId);
        stringRedisTemplate.expire(TOKEN_VERSION_PREFIX + userId,
                refreshTokenValidity, TimeUnit.MILLISECONDS);
    }

    /** 无 tv 的历史 token 按版本 0 处理，保证向后兼容 */
    private long tokenVersionOf(Map<String, Object> claims) {
        Object tv = claims.get("tv");
        return tv == null ? 0L : ((Number) tv).longValue();
    }
}