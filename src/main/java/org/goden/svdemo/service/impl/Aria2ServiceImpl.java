package org.goden.svdemo.service.impl;

import org.goden.svdemo.service.Aria2Service;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class Aria2ServiceImpl implements Aria2Service {
    private final String ARIA2_RPC_URL = "http://localhost:6800/jsonrpc";

    public String addUri(String magnetLink) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "1");
        request.put("method", "aria2.addUri");
        request.put("params", new Object[]{
                "token:YOUR_SECRET",  // 若aria2配置了RPC密钥
                new String[]{magnetLink}
        });

        Map response = restTemplate.postForObject(ARIA2_RPC_URL, request, Map.class);
        return (String) ((Map)response.get("result")).get("gid");
    }
}
