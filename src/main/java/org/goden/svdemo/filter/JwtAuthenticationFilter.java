package org.goden.svdemo.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.goden.svdemo.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader("Authorization");

        try {
            if (token != null) {
                // 1. 解析Token (与原Interceptor逻辑一致)
                Map<String, Object> user = jwtService.parseToken(token);
                if (user == null) {
                    throw new JWTVerificationException("Token验证失败");
                }

                // 2. 创建Authentication对象并存入SecurityContextHolder (替代ThreadLocal)
                // 这里简单地将用户ID作为principal。您可以根据需要构建更复杂的UserDetails对象。
                String userId = (String) user.get("userId"); // 假设token中包含userId字段
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // 继续执行过滤器链
            filterChain.doFilter(request, response);
        } catch (TokenExpiredException e) {
            // token过期
            returnUnauthorized(response, "登录已过期,请重新登录");
        } catch (JWTVerificationException e) {
            // token验证失败
            returnUnauthorized(response, "Token验证失败");
        } finally {
            // 请求结束后清理SecurityContext (替代Interceptor的afterCompletion逻辑)
            // 通常由SecurityContextPersistenceFilter处理，但在此明确清理是良好实践
             SecurityContextHolder.clearContext();
        }
    }

    // 返回401响应的方法，与原Interceptor逻辑一致
    private void returnUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = Map.of(
                "code", 401,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}
