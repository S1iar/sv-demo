package org.goden.svdemo.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    //密钥
    private static final String KEY =  "SvDemoTest";

    public static String getToken(Map<String, Object> claims){
        return JWT.create()
                .withClaim("user", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))//12小时
                .sign(Algorithm.HMAC256(KEY));
    }


    public static Map<String, Object> parseToken(String token){
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }

    public static String refreshAccessToken(String token) throws JWTVerificationException {
        Map<String, Object> claims = parseToken(token);

        // 生成新的访问Token
        token  = JWT.create()
            .withClaim("user", claims)
            .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))//12小时
            .sign(Algorithm.HMAC256(KEY));

        return token;
    }


}
