package com.refresh.gptChat.service;

/**
 * @author Yangyang
 * @create 2024-04-03 17:17
 */
public interface messageService {
    /**
     * 获取 url和 apiKey
     */
    String[] extractApiKeyAndRequestUrl(String authorizationHeader);
}
