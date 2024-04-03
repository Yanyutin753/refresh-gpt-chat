package com.refresh.gptChat.service;

import java.io.IOException;

/**
 * @author Yangyang
 * @create 2024-04-03 17:09
 */
public interface tokenService {
    /**
     * 用于refresh_token 拿到 access_token
     *
     * @param key (refresh_token)
     * @return
     * @throws IOException
     */
    String getAccessToken(String key);
}
