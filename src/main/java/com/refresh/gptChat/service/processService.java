package com.refresh.gptChat.service;

import com.refresh.gptChat.pojo.Conversation;
import com.refresh.gptChat.pojo.Image;
import com.refresh.gptChat.pojo.Speech;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Yangyang
 * @create 2024-04-08 9:48
 */
public interface processService {
    /**
     * /v1/chat/completions
     * 如发现token过期
     * 重新回复问题
     */
    void chatManageUnsuccessfulResponse(ConcurrentHashMap<String, String> refreshTokenList, Response resp, String refreshToken, HttpServletResponse response, Conversation conversation, String chatUrl, String requestId);

    /**
     * /v1/image/completions
     * 如发现token过期
     * 重新回复问题
     */
    void imageManageUnsuccessfulResponse(ConcurrentHashMap<String, String> refreshTokenList, Response resp, String refreshToken, HttpServletResponse response, Image conversation, String imageUrl, String requestId);

    /**
     * /v1/audio/speech
     * 如发现token过期
     * 重新回复问题
     */
    void speechManageUnsuccessfulResponse(ConcurrentHashMap<String, String> refreshTokenList, Response resp, String refreshToken, HttpServletResponse response, Speech conversation, String speechUrl, String requestId);

    /**
     * /v1/audio/transcriptions
     * 如发现token过期
     * 重新回复问题
     */
    void audioManageUnsuccessfulResponse(ConcurrentHashMap<String, String> refreshTokenList, Response resp, String refreshToken, HttpServletResponse response, RequestBody fileBody, String filename, String model, String audioUrl, String requestId);
}
