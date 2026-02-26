package org.goden.svdemo.interceptors;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.goden.svdemo.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String token = request.getHeader("Authorization");

        try{
            Map<String, Object> claims = JwtUtil.parseToken(token);
            return claims != null;
        }catch (TokenExpiredException e) {
            String newToken = JwtUtil.refreshAccessToken(token);
            returnUnauthorized(response, "Token已过期",newToken);
            return false;
        } catch (JWTVerificationException e) {
            returnUnauthorized(response, "Token验证失败","");
            return false;
        } catch (Exception e){
            return false;
        }
    }

//    private void returnUnauthorized(HttpServletResponse response, String message) throws IOException {
//        response.setStatus(HttpStatus.UNAUTHORIZED.value());
//        response.setContentType("application/json;charset=UTF-8");
//
//        Map<String, Object> result = Map.of(
//                "code", 401,
//                "message", message,
//                "timestamp", System.currentTimeMillis()
//        );
//
//        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
//    }

    private void returnUnauthorized(HttpServletResponse response, String message, String newToken) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = Map.of(
                "code", 401,
                "message", message,
                "newToken", newToken,
                "timeStamp", System.currentTimeMillis()
        );

        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }

}
