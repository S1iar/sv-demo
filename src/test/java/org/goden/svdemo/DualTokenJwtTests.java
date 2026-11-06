package org.goden.svdemo;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.goden.svdemo.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 双 Token（Access + Refresh）全链路测试
 * 前置条件：本地 Redis 可用（localhost:6379）
 * 说明：仅使用隔离的测试 userId，版本号 key 在每个用例后清理；
 *       黑名单 key TTL 已缩短为秒级，会自然过期，无需手动清理
 */
@SpringBootTest(properties = {
        "jwt.access-token-validity=3000",   // 缩短时效以便触发过期分支；3s 保证 revoke 用例截断后仍有约 2s 黑名单余量
        "jwt.refresh-token-validity=3000",
        "jwt.refresh-grace-period=60000"    // 60s 宽限窗口，过期后仍可断言
})
class DualTokenJwtTests {

    private static final long TEST_USER_ID = 9_999_999L;
    private static final String TV_KEY = "jwt:tv:" + TEST_USER_ID;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${jwt.secret}")
    private String secret;

    private Map<String, Object> claims;

    @BeforeEach
    void setUp() {
        // 每个用例前重读当前版本号（改密用例会使版本 +1）
        claims = new HashMap<>();
        claims.put("id", TEST_USER_ID);
        claims.put("username", "dual-token-tester");
        claims.put("tv", jwtService.currentTokenVersion(TEST_USER_ID));
    }

    @AfterEach
    void cleanUp() {
        redisTemplate.delete(TV_KEY);
    }

    private String newAccessToken() {
        return jwtService.generateAccessToken(claims);
    }

    private String newRefreshToken() {
        return jwtService.generateRefreshToken(claims);
    }

    // ==================== 基础生成 / 解析 ====================

    @Test
    @DisplayName("1. 正常生成与解析：access/refresh 各自按对应类型校验通过")
    void generateAndParse() {
        Map<String, Object> user = jwtService.parseAccessToken(newAccessToken());
        assertThat(user).containsEntry("username", "dual-token-tester");
        assertThat(((Number) user.get("id")).longValue()).isEqualTo(TEST_USER_ID);

        Map<String, Object> user2 = jwtService.parseToken(newRefreshToken(), "refresh");
        assertThat(((Number) user2.get("id")).longValue()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("2. 类型校验：refresh 不能冒充 access，反之亦然")
    void typeMustMatch() {
        assertThatThrownBy(() -> jwtService.parseAccessToken(newRefreshToken()))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("类型");

        assertThatThrownBy(() -> jwtService.parseToken(newAccessToken(), "refresh"))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("类型");
    }

    // ==================== 旋转与重放 ====================

    @Test
    @DisplayName("3. 旋转：换出新的一对，旧 refresh 用一次即废，重放被拒")
    void rotateOnceThenBurned() {
        String rt = newRefreshToken();
        Map<String, String> pair = jwtService.rotateRefreshToken(rt);
        assertThat(pair.get("accessToken")).isNotBlank();
        assertThat(pair.get("refreshToken")).isNotBlank();

        // 新 access 立即可用
        assertThat(jwtService.parseAccessToken(pair.get("accessToken")))
                .containsEntry("username", "dual-token-tester");

        // 旧 refresh 重放 → 拒绝
        assertThatThrownBy(() -> jwtService.rotateRefreshToken(rt))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("失效");
    }

    @Test
    @DisplayName("4. 旋转防御：access 冒充 / 伪造签名均被拒，且不污染黑名单")
    void rotateRejectsWrongSource() {
        assertThatThrownBy(() -> jwtService.rotateRefreshToken(newAccessToken()))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("类型");

        String forgedJti = UUID.randomUUID().toString();
        String forged = JWT.create()
                .withClaim("user", claims)
                .withClaim("type", "refresh")
                .withJWTId(forgedJti)
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(Algorithm.HMAC256("attacker-guessing-secret"));

        assertThatThrownBy(() -> jwtService.rotateRefreshToken(forged))
                .isInstanceOf(JWTVerificationException.class);
        // 验签先于抢占：伪造串的 jti 不应被写入黑名单
        assertThat(jwtService.isRevoked(forgedJti)).isFalse();
    }

    @Test
    @DisplayName("5. fail-closed：缺 jti 或 exp 的合法签名 refresh 被拒，防止无限旋转")
    void rotateFailClosed() {
        Algorithm alg = Algorithm.HMAC256(secret);

        String noJti = JWT.create()
                .withClaim("user", claims).withClaim("type", "refresh")
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(alg);
        assertThatThrownBy(() -> jwtService.rotateRefreshToken(noJti))
                .hasMessageContaining("无效");

        String noExp = JWT.create()
                .withClaim("user", claims).withClaim("type", "refresh")
                .withJWTId(UUID.randomUUID().toString())
                .sign(alg);
        assertThatThrownBy(() -> jwtService.rotateRefreshToken(noExp))
                .hasMessageContaining("无效");
    }

    // ==================== 登出吊销 ====================

    @Test
    @DisplayName("6. 登出吊销：revoke 后 access 不可用，refresh 不可旋转")
    void revokeOnLogout() {
        String at = newAccessToken();
        jwtService.parseAccessToken(at);  // 吊销前可用
        jwtService.revokeAccessToken("Bearer " + at);
        assertThatThrownBy(() -> jwtService.parseAccessToken(at))
                .hasMessageContaining("失效");

        String rt = newRefreshToken();
        jwtService.revokeRefreshToken(rt);
        assertThatThrownBy(() -> jwtService.rotateRefreshToken(rt))
                .hasMessageContaining("失效");
    }

    // ==================== 改密全端失效（核心需求） ====================

    @Test
    @DisplayName("7. 改密作废：旧 access 被拒、旧 refresh 不可旋转，重新登录换新版本后恢复正常")
    void passwordChangeInvalidatesBothTokens() {
        String at = newAccessToken();
        String rt = newRefreshToken();
        jwtService.parseAccessToken(at);  // 改密前一切正常

        // 模拟 updatePassword 成功后的服务端动作：版本号 +1
        jwtService.invalidateUserTokens(TEST_USER_ID);

        assertThatThrownBy(() -> jwtService.parseAccessToken(at))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("重新登录");
        assertThatThrownBy(() -> jwtService.rotateRefreshToken(rt))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("重新登录");

        // 重新登录：携带新版本号签发的 token 恢复可用
        Map<String, Object> reLoginClaims = new HashMap<>(claims);
        reLoginClaims.put("tv", jwtService.currentTokenVersion(TEST_USER_ID));
        String newAt = jwtService.generateAccessToken(reLoginClaims);
        assertThat(jwtService.parseAccessToken(newAt))
                .containsEntry("username", "dual-token-tester");
    }

    // ==================== 过期与宽限期 ====================

    @Test
    @DisplayName("8. access 过期：抛 TokenExpiredException（前端据此走静默刷新的信号）")
    void accessTokenExpired() throws InterruptedException {
        String at = newAccessToken();
        Thread.sleep(3100);
        assertThatThrownBy(() -> jwtService.parseAccessToken(at))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    @DisplayName("9. 宽限期：refresh 过期后仍可换一次，第二次重放被黑名单拦截")
    void gracePeriodSingleUse() throws InterruptedException {
        String rt = newRefreshToken();
        Thread.sleep(3100);  // refresh（3s）已过期，仍在 60s 宽限期内

        Map<String, String> pair = jwtService.rotateRefreshToken(rt);
        assertThat(pair.get("accessToken")).isNotBlank();

        assertThatThrownBy(() -> jwtService.rotateRefreshToken(rt))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("失效");
    }

    // ==================== 并发抢占 ====================

    @Test
    @DisplayName("10. 并发旋转：同一 refresh 两线程同时刷新，只允许一个成功")
    void concurrentRotateOnlyOneWins() throws Exception {
        String rt = newRefreshToken();
        int threads = 2;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successCount = new AtomicInteger();
        List<Exception> failures = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    barrier.await();
                    jwtService.rotateRefreshToken(rt);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failures.add(e);
                }
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures).hasSize(threads - 1)
                .allMatch(e -> e instanceof JWTVerificationException);
    }
}