package org.goden.svdemo.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;

/**
 * Refresh Token 的 HttpOnly Cookie 构造器。
 *
 * <h2>为什么 refresh 一定要走 Cookie 而不是响应体</h2>
 * <ul>
 *   <li>HttpOnly → JS 读不到，XSS 拿不走长效凭证；access 存 localStorage 只被影响 15 分钟。</li>
 *   <li>SameSite=Lax → 阻断 CSRF（跨站 POST 不会自动带 Cookie）。</li>
 *   <li>Path=/user → 只在 refresh/logout 端点被发出，减少暴露面。</li>
 * </ul>
 *
 * <h2>跨域部署注意</h2>
 * 前后端不同域时（如 localhost:3000 ↔ localhost:8080），必须 {@code sameSite=None + secure=true}，
 * 且后端加 CORS 允许 credentials；本地 http 调试无法用 secure，所以默认 Lax。
 */
@Component
public class AuthCookieHelper {

    /** Cookie 名；短一点便于 Set-Cookie 头节省字节。 */
    public static final String RT_NAME = "rt";

    @Value("${auth.cookie.secure:false}")
    private boolean secure;

    @Value("${auth.cookie.same-site:Lax}")
    private String sameSite;

    /** 必须覆盖 /user/refresh 和 /user/logout，别只写 /user/refresh */
    @Value("${auth.cookie.path:/user}")
    private String path;

    /** 直接复用 JWT 的有效期配置，避免 Cookie 寿命和 token 寿命两处配错 */
    @Value("${jwt.refresh-token-validity:604800000}")
    private long refreshValidityMs;

    /** 签发/旋转成功后调用：写入新 refresh。 */
    public ResponseCookie build(String refreshToken) {
        return base(refreshToken == null ? "" : refreshToken,
                Duration.ofMillis(refreshValidityMs)).build();
    }

    /** 登出时调用：value 为空 + maxAge=0，浏览器立即清除。 */
    public ResponseCookie clear() {
        return base("", Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value, Duration age) {
        return ResponseCookie.from(RT_NAME, value)
                .httpOnly(true)        // JS 读不到，XSS 拿不到长效凭证
                .secure(secure)        // 生产 HTTPS 改 true
                .sameSite(sameSite)    // 真跨站改 None，且 secure 必须 true
                .path(path)
                .maxAge(age);
    }
}