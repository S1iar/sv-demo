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

    // 定义不需要JWT验证的路径
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/user/login",
            "/user/register"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 去掉contextPath后精确匹配，避免 /xxx/user/login 这类路径被误放行
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
                // 1. 解析Token
                Map<String, Object> user = jwtService.parseToken(token);

                String username = (String) user.get("username");
                // JWT的claim反序列化后小整数是Integer，不能直接强转Long
                Long id = ((Number) user.get("id")).longValue();
                CustomUserDetails userDetails = userDetailsService.loadUserByUsername(username);
                userDetails.setId(id);

                // 2. 创建Authentication对象并存入SecurityContextHolder (替代ThreadLocal)
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }else {
                //未携带token
                returnUnauthorized(response, "账号未登录!");
                return;  // 终止过滤器链
            }
            // 继续执行过滤器链
            filterChain.doFilter(request, response);
        } catch (TokenExpiredException e) {
            // token过期
            returnUnauthorized(response, "登录已过期,请重新登录!");
        } catch (UsernameNotFoundException e) {
            // token有效但用户已不存在
            returnUnauthorized(response, "账号不存在或已被删除!");
        } catch (JWTVerificationException e) {
            // token验证失败
            returnUnauthorized(response, "Token验证失败!");
        }
    }

    // 返回401响应的方法
    private void returnUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = Map.of(
                "code", 401,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
    }
}
