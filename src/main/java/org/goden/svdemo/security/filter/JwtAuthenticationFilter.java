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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /** 不需要 JWT 验证的路径 */
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/user/login",
            "/user/register",
            "/user/refresh"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return EXCLUDED_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                // 1. 解析并校验 token（验签 + 黑名单）
                Map<String, Object> user = jwtService.parseToken(token);

                String username = (String) user.get("username");
                // JWT claim 反序列化后小整数为 Integer，不可直接强转 Long
                Long id = ((Number) user.get("id")).longValue();
                CustomUserDetails userDetails = userDetailsService.loadUserByUsername(username);
                userDetails.setId(id);

                // 2. 写入 SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                returnUnauthorized(response, "账号未登录!");
                return;
            }
            filterChain.doFilter(request, response);
        } catch (TokenExpiredException e) {
            // access token 过期，前端应拿 refresh token 去 /user/refresh 换新
            returnJson(response, HttpStatus.UNAUTHORIZED.value(), "access_token_expired", "登录已过期,请重新登录!");
        } catch (UsernameNotFoundException e) {
            returnUnauthorized(response, "账号不存在或已被删除!");
        } catch (JWTVerificationException e) {
            returnUnauthorized(response, e.getMessage());
        }
    }

    private void returnUnauthorized(HttpServletResponse response, String message) throws IOException {
        returnJson(response, HttpStatus.UNAUTHORIZED.value(), "401", message);
    }

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
