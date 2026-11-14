package org.goden.svdemo.service;

import com.auth0.jwt.exceptions.JWTVerificationException;
import java.util.Map;

/**
 * 双 Token（Access + Refresh）认证服务。
 *
 * <h2>整体生命周期</h2>
 * <pre>
 *   登录            每次业务请求            Access 过期(15min)         Refresh 过期(7d)
 *     │                   │                        │                          │
 *     ▼                   ▼                        ▼                          ▼
 *  login()             Filter 校验              前端调 /user/refresh         强制重新登录
 *  发 access+refresh   parseAccessToken        rotateRefreshToken            login()
 *  存 localStorage     (Bearer Header)         旧 jti 进 SETNX 黑名单         签发新的一对
 *  + HttpOnly Cookie   ↓                       签发新的一对
 *                      类型=access？             ↑ Cookie 里读旧 refresh
 *                      jti 不在黑名单？           ↓
 *                      tv 与 Redis 当前版本一致？ 前端替换 localStorage 中 access
 *                                                  浏览器更新 Set-Cookie
 * </pre>
 *
 * <h2>三条安全防线</h2>
 * <ol>
 *   <li><b>类型隔离</b>：refresh 不能当 access 用（避免长效凭证直接访问业务接口）。</li>
 *   <li><b>一次性旋转</b>：refresh 每次使用都进黑名单（SETNX 抢占，防并发重放）。</li>
 *   <li><b>版本作废</b>：per-user {@code tv} claim 与 Redis {@code jwt:tv:{uid}} 比对，
 *       改密时 INCR 一次即可让该用户所有历史 token 立即失效。</li>
 * </ol>
 */
public interface JwtService {

    // ============ 签发 ============

    /** 生成 access token（默认 15min），用于业务接口鉴权。claims 里必须包含 id/username/tv。 */
    String generateAccessToken(Map<String, Object> claims);

    /** 生成 refresh token（默认 7d），只用于换 access；通过 HttpOnly Cookie 下发。 */
    String generateRefreshToken(Map<String, Object> claims);

    // ============ 校验 ============

    /**
     * 解析并校验 token：验签 → 类型匹配 → 黑名单 → 版本号。
     *
     * @param token      JWT 字符串
     * @param expectType "access" 或 "refresh"；null 表示不校验类型（不推荐）
     * @return user claim（含 id/username/tv）
     * @throws com.auth0.jwt.exceptions.TokenExpiredException token 已过期
     * @throws JWTVerificationException                       验签/类型/黑名单/版本任一失败
     */
    Map<String, Object> parseToken(String token, String expectType);

    /** Filter 便捷方法：只走 access 校验分支。 */
    default Map<String, Object> parseAccessToken(String token) {
        return parseToken(token, "access");
    }

    // ============ 旋转 ============

    /**
     * 用旧 refresh 换新的 access + refresh 一对。
     * <p>顺序：decode（不验签，只读元数据） → <b>验签</b> → <b>SETNX 抢占旧 jti</b> → 签发新对。
     * 顺序不能变：验签在前防止伪造串污染 Redis；抢占在签发前做到并发下"用一次即废"。
     * <p>过期但在宽限期内（{@code jwt.refresh-grace-period}）仍允许换一次，之后即被拉黑。
     *
     * @return {@code {accessToken, refreshToken}}
     * @throws JWTVerificationException 任何失败场景均抛此异常，由 Controller 转 401
     */
    Map<String, String> rotateRefreshToken(String refreshToken) throws JWTVerificationException;

    // ============ 吊销（登出/踢下线） ============

    /** 底层：把指定 jti 加入黑名单，TTL 应覆盖其剩余寿命。 */
    void revokeToken(String jti, long ttlSeconds);

    /** 登出：吊销 access（Bearer 前缀自动剥离；已过期也无所谓）。 */
    void revokeAccessToken(String token);

    /** 登出：吊销 refresh（HttpOnly Cookie 里的裸串；过期宽限期内的也会拉黑）。 */
    void revokeRefreshToken(String refreshToken);

    /** 查询某 jti 是否已被吊销（供 parseToken 使用）。 */
    boolean isRevoked(String jti);

    // ============ 版本号（改密全端失效） ============

    /**
     * 读取用户当前 token 版本号；<b>会滑动续期 Redis Key 的 TTL</b>，仅供签发/旋转等
     * "写路径"调用。业务校验热路径请使用只读版本，避免每请求一次 EXPIRE 命令。
     * <p>Redis 无记录时返回 0，向后兼容历史无 tv 的 token。
     */
    long currentTokenVersion(long userId);

    /**
     * 版本号 +1：使该用户所有已签发的 access/refresh 立即全部失效。
     * <p>典型调用时机：{@code UserServiceImpl.updatePassword} 成功后。
     */
    void invalidateUserTokens(long userId);
}