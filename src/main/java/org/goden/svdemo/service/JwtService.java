package org.goden.svdemo.service;

import java.util.Map;

public interface JwtService {
    String getToken(Map<String, Object> claims);

    Map<String, Object> parseToken(String token);

    String refreshAccessToken(String token);
}
