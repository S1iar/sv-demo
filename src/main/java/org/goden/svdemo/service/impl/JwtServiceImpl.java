package org.goden.svdemo.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.goden.svdemo.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class JwtServiceImpl implements JwtService {

    //密钥
//    private static final String secretKey =  "SvDemoTest";
    //密钥从application.yml或者环境变量获取
    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public String generateToken(Map<String, Object> claims){
        return JWT.create()
                .withClaim("user", claims)
                // 签发时间
                .withIssuedAt(new Date())
                // 24小时过期时间
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .sign(Algorithm.HMAC256(secretKey));
    }

    @Override
    public Map<String, Object> parseToken(String token){
        return JWT.require(Algorithm.HMAC256(secretKey))
                .build()
                .verify(token)
                .getClaim("user")
                .asMap();
    }

    @Override
    public String refreshAccessToken(String token) throws JWTVerificationException {
        try {
            Map<String, Object> claims = parseToken(token);
            return generateToken(claims);
        } catch (TokenExpiredException e) {
            // 如果过期，允许在宽限期内刷新（例如过期后5分钟内）
            DecodedJWT decoded = JWT.decode(token);
            Date expiresAt = decoded.getExpiresAt();
            if (expiresAt != null && System.currentTimeMillis() - expiresAt.getTime() <= 300_000) {
                Map<String, Object> claims = decoded.getClaim("user").asMap();
                return generateToken(claims);
            }
            throw e;
        }
    }
}
