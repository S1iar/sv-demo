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

/**
 * 双 Token 认证的核心实现。
 *
 * <h2>Redis Key 约定</h2>
 * <ul>
 *   <li>{@code jwt:blacklist:<jti>}：已吊销的 token，值为 "1"，TTL = 剩余寿命。</li>
 *   <li>{@code jwt:tv:<userId>}：该用户当前的 token 版本号；改密时 INCR，签发/旋转时读取。</li>
 * </ul>
 *
 * <h2>JWT Claim 结构</h2>
 * <pre>
 * {
 *   "user": { "id": 1, "username": "xxx", "tv": 3 },  // 业务信息 + 版本号
 *   "type": "access" | "refresh",                    // 类型隔离：refresh 不能直接访问业务接口
 *   "jti": "<uuid>",                                 // 唯一标识，黑名单以它为键
 *   "iat": ..., "exp": ...                           // 标准时间戳
 * }
 * </pre>
 *
 * <h2>为什么这么设计</h2>
 * <ol>
 *   <li><b>Access 存 localStorage</b>：短时效，被 XSS 偷走危害窗口小；由前端加到 Authorization Header。</li>
 *   <li><b>Refresh 走 HttpOnly Cookie</b>：JS 读不到，XSS 拿不走长效凭证；Path=/user 覆盖 refresh/logout。</li>
 *   <li><b>SETNX 抢占黑名单</b>：并发同一 refresh 打两次，只有一个能写入 key，另一个直接判为重放。</li>
 *   <li><b>Per-user 版本号</b>：不需要维护"用户→jti 列表"这种会膨胀的索引；INCR 一次即全量作废。</li>
 *   <li><b>宽限期</b>：网络抖动导致 refresh 刚好过期时，仍允许换一次；旧 jti 会立即拉黑，防重放。</li>
 * </ol>
 */
@Service
public class JwtServiceImpl implements JwtService {

    // 生产环境务必通过环境变量 JWT_SECRET 注入；长度需 ≥32 字节满足 HMAC256
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity:900000}")   // 15 分钟
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity:604800000}") // 7 天
    private long refreshTokenValidity;

    @Value("${jwt.refresh-grace-period:900000}")     // 15 分钟宽限，覆盖网络抖动/时钟漂移
    private long refreshGracePeriod;

    private final StringRedisTemplate stringRedisTemplate;

    /** 统一黑名单前缀，避免 Redis key 污染 */
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /** per-user token 版本号，改密后自增使旧 token 全量作废 */
    private static final String TOKEN_VERSION_PREFIX = "jwt:tv:";

    private Algorithm algorithm;

    public JwtServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** HMAC256 算法对象在字段注入完成后才能构建，因此放到 @PostConstruct 而不是构造函数。 */
    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(secretKey);
    }

    // ==================== Token 生成 ====================

    /**
     * 签发 access。
     * <p>调用时机：登录成功后、或 refresh 旋转成功后。
     * <p>注意：{@code claims} 里已经带了 {@code tv}（由 {@code UserServiceImpl.login} 或
     * {@code rotateRefreshToken} 保证），这里不再单独注入版本号，避免双写。
     */
    @Override
    public String generateAccessToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("user", claims)                     // 业务信息 + tv
                .withClaim("type", "access")                   // 类型隔离标记
                .withJWTId(UUID.randomUUID().toString())       // jti：黑名单/吊销都以此为准
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenValidity))
                .sign(algorithm);
    }

    /** 签发 refresh。结构与 access 相同，仅 {@code type} 与有效期不同。 */
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

    /**
     * 校验链路（任一失败即抛异常，全部通过才返回 user claim）：
     * <pre>
     *   ① 验签 + 有效期（auth0 库内置）
     *   ② 类型必须匹配（refresh 冒充 access 会被拒）
     *   ③ 黑名单：jti 已被吊销 → 拒
     *   ④ 版本号：token.tv ≠ Redis 当前版本 → 拒（改密后全端下线）
     * </pre>
     * 顺序不能变：验签必须最靠前，避免伪造串消耗后续 Redis 查询。
     */
    @Override
    public Map<String, Object> parseToken(String token, String expectType) {
        // ① 验签 + exp；过期会抛 TokenExpiredException，由 Filter 单独映射成 access_token_expired
        DecodedJWT decoded = JWT.require(algorithm).build().verify(token);

        // ② 类型校验：refresh 不能当 access 用，反之亦然
        if (expectType != null) {
            String type = decoded.getClaim("type").asString();
            if (!expectType.equals(type)) {
                throw new JWTVerificationException("Token类型错误");
            }
        }

        // ③ 黑名单：登出或旋转已烧掉的 jti 走这里被拦下
        String jti = decoded.getId();
        if (jti != null && isRevoked(jti)) {
            throw new JWTVerificationException("Token已失效，请重新登录");
        }

        Map<String, Object> user = decoded.getClaim("user").asMap();
        if (user == null) {
            throw new JWTVerificationException("Token验证失败");
        }

        // ④ 版本号校验：改密后旧 access 立即失效
        //    热路径：只读，不做 EXPIRE 续期；续期交由 login / rotateRefreshToken 负责
        Object userId = user.get("id");
        if (userId != null && peekTokenVersion(((Number) userId).longValue()) != tokenVersionOf(user)) {
            throw new JWTVerificationException("登录状态已失效，请重新登录!");
        }
        return user;
    }

    // ==================== Refresh 旋转 ====================

    /**
     * Refresh 旋转：用旧 refresh 换新的 access + refresh 一对。
     *
     * <p><b>四步顺序不可颠倒</b>（这是本方法最核心的安全设计）：
     * <pre>
     *   ① decode（不验签，仅读元数据）
     *      → 提前拿到 jti/exp/user，用于 fail-closed 检查
     *   ② 验签
     *      → 若伪造串跳过此步直接写黑名单，会撑爆 Redis（历史 DoS 教训）
     *   ③ SETNX 抢占旧 jti
     *      → 并发下只有一个线程能抢到，抢不到的视为重放；这一步也实现"用一次即废"
     *   ④ 签发新的一对
     *      → 若签发失败也不影响安全性：旧 jti 已烧，最坏情况让用户重新登录（Fail-Safe）
     * </pre>
     *
     * <p><b>宽限期</b>：exp 已过但在 {@code refreshGracePeriod} 内仍允许换一次；
     * 应对前端刚拿到 access、网络断了、重试时 access 已过期，refresh 也刚好过期的场景。
     */
    @Override
    public Map<String, String> rotateRefreshToken(String refreshToken) throws JWTVerificationException {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new JWTVerificationException("缺少refresh token");
        }

        // ① 先解出基本信息（不验签，仅取元数据）
        //    注意：这里 decode 出来的字段仅用于类型/存在性判断，绝不能作为业务凭证使用
        DecodedJWT decoded;
        try {
            decoded = JWT.decode(refreshToken);
        } catch (Exception e) {
            // Base64 结构都不对，直接拒
            throw new JWTVerificationException("无效的refresh token");
        }
        // 类型必须是 refresh：即使攻击者偷到一个 access 也不能用来旋转
        if (!"refresh".equals(decoded.getClaim("type").asString())) {
            throw new JWTVerificationException("Token类型错误");
        }
        String oldJti = decoded.getId();
        Date oldExpiresAt = decoded.getExpiresAt();
        Map<String, Object> claims = decoded.getClaim("user").asMap();

        // fail-closed：合法 refresh 一定带 jti 和 exp，缺失则拒绝，防止绕过黑名单无限旋转
        //（若允许无 jti 的 refresh 通过，攻击者可反复旋转而不占用任何黑名单 key）
        if (oldJti == null || oldJti.isBlank() || oldExpiresAt == null) {
            throw new JWTVerificationException("无效的refresh token");
        }

        // ② 先验签：防止伪造/篡改的串污染黑名单
        DecodedJWT verified = null;
        try {
            verified = JWT.require(algorithm).build().verify(refreshToken);
        } catch (TokenExpiredException e) {
            // 已过期：仅宽限期内、且 claims 完整才允许
            boolean inGrace = System.currentTimeMillis() - oldExpiresAt.getTime() <= refreshGracePeriod;
            if (!inGrace || claims == null) {
                throw new JWTVerificationException("Refresh Token已过期，请重新登录");
            }
            // 走到这里：签名合法、只是过期、且在宽限期内 → 继续走抢占
        } catch (JWTVerificationException e) {
            // 签名不对 / 结构异常 → 拒；不做任何 Redis 写入
            throw new JWTVerificationException("Token验证失败");
        }

        if (claims == null) {
            // 只有当上面的 verify 未过期成功、但 user claim 缺失时才会到这里
            throw new JWTVerificationException("Token验证失败");
        }
        // 保险：若上面 verify 未抛异常，使用其解码结果（保证 exp 等字段是签名保护过的）
        if (verified != null) {
            oldExpiresAt = verified.getExpiresAt();
        }

        // 版本号校验：改密后旧 refresh 不允许再旋转（在抢占黑名单前检查，避免误烧 jti）
        //   ——若先抢占再校验版本，攻击者拿着被作废的 refresh 打一次就会消耗掉一次黑名单名额，
        //     但用户合法旋转也会被同样拒绝；先校验再抢占可以让非法请求根本不落到 Redis。
        Object uid = claims.get("id");
        if (uid != null && currentTokenVersion(((Number) uid).longValue()) != tokenVersionOf(claims)) {
            throw new JWTVerificationException("登录状态已失效，请重新登录!");
        }

        // ③ 抢占黑名单（SETNX）：验签通过后才写，解决并发重放
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

        // ④ 签发新的一对；旧 refresh 已烧，新 refresh 有全新 jti
        //    注意：claims 里的 tv 是从旧 token 继承的，且上面已验证与当前版本一致，
        //          所以新 token 直接携带同一 tv 即可。
        Map<String, String> pair = new HashMap<>();
        pair.put("accessToken", generateAccessToken(claims));
        pair.put("refreshToken", generateRefreshToken(claims));
        return pair;
    }

    /**
     * 计算剩余有效期（秒），已过期或空返回 &lt;=0。
     * <p>用途：写黑名单时用剩余寿命做 TTL，避免为早已失效的 token 永久占位。
     */
    private long claimBlacklistTtl(Date expiresAt) {
        if (expiresAt == null) return -1;
        return (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
    }

    // ==================== 黑名单操作 ====================

    /** 底层：向黑名单写一个 jti。ttl &lt;= 0 时不写（该 token 已自然过期，无需占位）。 */
    @Override
    public void revokeToken(String jti, long ttlSeconds) {
        if (jti != null && ttlSeconds > 0) {
            stringRedisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        }
    }

    /** 登出场景：吊销 access。前端可能带也可能不带 Bearer 前缀，方法内自动剥离。 */
    @Override
    public void revokeAccessToken(String token) {
        revokeByPrefix(token, "Bearer ");
    }

    /**
     * 登出场景：吊销 refresh。
     * <p>refresh 只走 HttpOnly Cookie，是裸串（无前缀）；保留 decode 不验签以兼容已过期的登出场景
     * ——用户可能几天没打开 App，refresh 已过期，此时仍应允许点击登出把 Cookie 清干净。
     */
    @Override
    public void revokeRefreshToken(String refreshToken) {
        revokeByPrefix(refreshToken, null);
    }

    /**
     * 统一处理：可带前缀、可带裸串；<b>不验签</b>，只用 decode。
     * <p>为什么不验签：登出本身是无害操作，即便 access 已过期也应允许；且此时 token 已经
     * 经过 Filter 或 Spring Security 的路由白名单，不做二次签名检查。
     * <p>潜在风险（伪造串污染 Redis）由"必须能 decode 出合法 jti 才写"部分缓解；
     * 若担心 DoS，可先 verify 签名/仅允许 TokenExpiredException 通过（详见 review 记录）。
     */
    private void revokeByPrefix(String token, String prefix) {
        if (token == null || token.isEmpty()) return;
        if (prefix != null && token.startsWith(prefix)) {
            token = token.substring(prefix.length()).trim();
        }
        try {
            DecodedJWT decoded = JWT.decode(token);
            long ttl = claimBlacklistTtl(decoded.getExpiresAt());
            if (ttl <= 0) {
                // 兜底：过期 token 也要拉黑一段时间，覆盖 refresh-grace-period，
                // 避免"过期但仍在宽限期"的 refresh 被登出后依然能旋转
                ttl = refreshGracePeriod / 1000;
            }
            revokeToken(decoded.getId(), ttl);
        } catch (Exception ignored) {
            // 伪造/格式错，忽略，登出仍算成功
        }
    }

    /** 查询 jti 是否已被吊销。Boolean.TRUE.equals 防御 Redis pipeline/事务场景下的 null 返回值。 */
    @Override
    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    // ==================== 版本号 ====================

    /**
     * <b>写路径专用</b>：读取用户当前 token 版本号，并滑动续期 Redis Key。
     * <p>只在 {@code login} 和 {@code rotateRefreshToken} 中调用——这两个时刻本身就是"签发新 token"，
     * 顺带把版本号 key 的寿命拉到 7 天，确保任何携带该版本的 token 在使用期间 key 不会消失。
     * <p>校验热路径（{@code parseToken}）请用 {@link #peekTokenVersion}，避免每请求一次 EXPIRE。
     * <p>Key 不存在时返回 0：向后兼容旧版本签发的无 tv token，也让"从未改过密"的用户默认在版本 0。
     */
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

    /**
     * <b>读路径专用</b>：只读版本号，不产生 Redis 写命令。
     * <p>语义与 {@link #currentTokenVersion} 一致（Key 不存在按 0 处理），仅少了 EXPIRE 副作用。
     */
    private long peekTokenVersion(long userId) {
        String v = stringRedisTemplate.opsForValue().get(TOKEN_VERSION_PREFIX + userId);
        return v == null ? 0L : Long.parseLong(v);
    }

    /**
     * 使某用户所有已签发的 token 立即失效：版本号 +1，之后旧 token 的 tv 就对不上了。
     * <p>调用方：{@code UserServiceImpl.updatePassword}。
     * <p>INCR 若发现 key 不存在会自动创建（值=1），因此用户从"版本 0"改密后自然变"版本 1"。
     * <p>不主动删旧黑名单 key：让其自然按剩余寿命过期，Redis 内存有界。
     */
    @Override
    public void invalidateUserTokens(long userId) {
        // 旧 token 最多还能存活一个 refresh 周期，TTL 覆盖它即可
        stringRedisTemplate.opsForValue().increment(TOKEN_VERSION_PREFIX + userId);
        stringRedisTemplate.expire(TOKEN_VERSION_PREFIX + userId,
                refreshTokenValidity, TimeUnit.MILLISECONDS);
    }

    /** 无 tv 的历史 token 按版本 0 处理，保证向后兼容。 */
    private long tokenVersionOf(Map<String, Object> claims) {
        Object tv = claims.get("tv");
        // 反序列化后小整数可能是 Integer，用 Number.longValue 兼容
        return tv == null ? 0L : ((Number) tv).longValue();
    }
}