package com.refresh.gptChat.service;

import com.refresh.gptChat.pojo.Conversation;
import okhttp3.Response;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.CompletableFuture;

/**
 * @author Yangyang
 * @create 2024-04-03 17:07
 */
public interface outPutService {
    /**
     * chat接口的输出
     *
     * @param response
     * @param resp
     * @param conversation
     */
    void outPutChat(HttpServletResponse response, Response resp, Conversation conversation);

    /**
     * 返回异步responseEntity
     *
     * @param response future
     */
    ResponseEntity<Object> getObjectResponseEntity(HttpServletResponse response, CompletableFuture<ResponseEntity<Object>> future);

    /**
     * image接口的输出
     *
     * @param response
     * @param resp
     */
    void outPutImage(HttpServletResponse response, Response resp, Conversation conversation);
}
