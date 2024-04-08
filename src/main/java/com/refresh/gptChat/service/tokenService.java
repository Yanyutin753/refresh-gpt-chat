package com.refresh.gptChat.service;

import java.util.Map;

/**
 * @author Yangyang
 * @create 2024-04-03 17:09
 */
public interface tokenService {
    /**
     * 用于refresh_token 拿到 access_token
     */
    String getAccessToken(String key);

    /**
     * 用于添加请求头
     */
    Map<String, String> addHeader(String accessToken, String requestId);
}
