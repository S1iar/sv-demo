package org.goden.svdemo.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class AuthCookieHelper {

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

    public ResponseCookie build(String refreshToken) {
        return base(refreshToken == null ? "" : refreshToken,
                Duration.ofMillis(refreshValidityMs)).build();
    }

    /** 清 Cookie：name / path / secure / sameSite 必须与 build() 完全一致 */
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