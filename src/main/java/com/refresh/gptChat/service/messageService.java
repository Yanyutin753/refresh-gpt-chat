package com.refresh.gptChat.service;

import com.refresh.gptChat.pojo.Conversation;

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
     * @param conversation
     * @throws IOException
     */
    String[] extractApiKeyAndRequestUrl(String authorizationHeader, Conversation conversation);
}
