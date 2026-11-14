package org.goden.svdemo.security.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.goden.svdemo.security.userdetails.CustomUserDetails;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.security.service.UserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

/**
 * Access Token 认证过滤器。
 *
 * <h2>职责</h2>
 * 对每一个受保护的请求：从 Authorization Header 取出 access token → 走 {@code parseAccessToken}
 * 的四层校验（验签 / 类型 / 黑名单 / 版本号）→ 通过后把 {@link CustomUserDetails} 塞进
 * SecurityContext，让后续的 {@code @PreAuthorize} 能读到权限。
 *
 * <h2>不处理的场景</h2>
 * <ul>
 *   <li>Refresh Token 的旋转：由 {@code UserController#refresh} 直接调 {@code rotateRefreshToken}。</li>
 *   <li>登出：{@code /user/logout} 走白名单，允许 access 已过期时也能清 Cookie。</li>
 * </ul>
 *
 * <h2>前端错误码约定</h2>
 * 响应体中的 {@code code} 字段决定前端行为：
 * <ul>
 *   <li>{@code access_token_expired} → 触发静默刷新（调 /user/refresh）</li>
 *   <li>{@code token_invalid} → 视为彻底失效（含黑名单/版本作废），清 localStorage 跳登录</li>
 *   <li>{@code 401} → 账号不存在/被删除，直接跳登录</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 白名单：这些路径完全不进本 Filter。
     * <p>logout 之所以能过：Cookie 里的 refresh 需要拉黑 + 前端 localStorage 里的 access 需要拉黑，
     * 但都不要求 access 本身还有效。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/user/login")
                || path.equals("/user/register")
                || path.equals("/user/refresh")
                || path.equals("/user/logout");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        try {
            // 只有带 Bearer 头的请求才做鉴权；未带 token 的请求走 Spring Security 的
            // authenticationEntryPoint（anonymous → 401）
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                // 只接受 access 类型；refresh 走 /user/refresh，不允许直接访问业务接口
                Map<String, Object> user = jwtService.parseAccessToken(token);

                // 二次查库：拿到当前权限列表（角色/细粒度权限可能在 token 签发后变更）
                String username = (String) user.get("username");
                Long id = ((Number) user.get("id")).longValue();
                CustomUserDetails userDetails = userDetailsService.loadUserByUsername(username);
                userDetails.setId(id);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);

        } catch (TokenExpiredException e) {
            // 前端识别这个 code 才会去走 /user/refresh；不要用泛 401
            returnJson(response, HttpStatus.UNAUTHORIZED.value(), "access_token_expired", "登录已过期,请刷新!");
        } catch (UsernameNotFoundException e) {
            // Token 合法但账号已不存在/已删除：无法通过刷新恢复，直接踢回登录
            returnUnauthorized(response, "账号不存在或已被删除!");
        } catch (JWTVerificationException e) {
            // 被拉黑的 token 应引导重新登录，而不是继续刷新
            returnJson(response, HttpStatus.UNAUTHORIZED.value(), "token_invalid", e.getMessage());
        }
    }

    private void returnUnauthorized(HttpServletResponse response, String message) throws IOException {
        returnJson(response, HttpStatus.UNAUTHORIZED.value(), "401", message);
    }

    /** 统一 JSON 错误响应体；code 是字符串，供前端做语义分支。 */
    private void returnJson(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = Map.of(
                "code", code,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
    }
}
