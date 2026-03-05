package org.goden.svdemo.interceptors;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.goden.svdemo.service.JwtService;
import org.goden.svdemo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String token = request.getHeader("Authorization");

        try{
            Map<String, Object> user = jwtService.parseToken(token);
            //当获取token中的用户信息为空则判错
            if(user == null) throw new JWTVerificationException("Token验证失败");
            //将用户信息放入ThreadLocal
            ThreadLocalUtil.set(user);

            return true;
        }catch (TokenExpiredException e) {
            // token过期
            returnUnauthorized(response, "登录已过期,请重新登录");
            return false;
        } catch (JWTVerificationException e) {
            returnUnauthorized(response, "Token验证失败");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }

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
