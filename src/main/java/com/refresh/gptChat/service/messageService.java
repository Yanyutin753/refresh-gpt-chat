package com.refresh.gptChat.service;

import java.io.IOException;

/**
 * @author Yangyang
 * @create 2024-04-03 17:17
 */
public interface messageService {
    /**
     * 获取url和apiKey
     *
     * @param authorizationHeader
     * @throws IOException
     */
    String[] extractApiKeyAndRequestUrl(String authorizationHeader);
}
