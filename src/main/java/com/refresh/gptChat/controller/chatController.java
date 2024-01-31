package com.refresh.gptChat.controller;

import com.alibaba.fastjson2.JSON;
import com.refresh.gptChat.chat.Conversation;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Yangyang
 * @create 2023-12-25 18:29
 */

@Slf4j
@RestController()
public class chatController {
    /**
     * 缓存cocopilotToken
     */
    private static final HashMap<String, String> refreshTokenList;

    static {
        refreshTokenList = new HashMap<>();
        log.info("初始化接口成功！");
    }

    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).build();
    private final ExecutorService executor = new ThreadPoolExecutor(0, 300, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    @Value("${getAccessTokenUrl}")
    private String getAccessTokenUrl;
    @Value("${chatUrl}")
    private String chatUrl;

    @Scheduled(cron = "0 0 0 * * ?")
    private void clearModelsUsage() {
        int count = 0;
        for (Map.Entry<String, String> entry : refreshTokenList.entrySet()) {
            String access_token = getAccessToken(entry.getKey());
            if (access_token != null) {
                refreshTokenList.put(entry.getKey(), access_token);
                count++;
            }
        }
        log.info("检查access_token成功：" + count + "失败：" + (refreshTokenList.size() - count));
    }

    /**
     * 请求体不是json 会报Request body is missing or not in JSON format
     * Authorization token缺失  会报Authorization header is missing
     * 无法请求到access_token 会报refresh_token is wrong
     *
     * @param response
     * @param request
     * @param conversation
     * @return
     * @throws JSONException
     * @throws IOException
     */
    @PostMapping(value = "/v1/chat/completions")
    public CompletableFuture<ResponseEntity<String>> coPilotConversation(HttpServletResponse response, HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody Conversation conversation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (conversation == null) {
                    return new ResponseEntity<>("Request body is missing or not in JSON format", HttpStatus.BAD_REQUEST);
                }
                String authorizationHeader = StringUtils.trimToNull(request.getHeader("Authorization"));
                String refresh_token;
                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                    refresh_token = authorizationHeader.substring(7);
                } else {
                    return new ResponseEntity<>("Authorization header is missing", HttpStatus.UNAUTHORIZED);
                }
                if (!refreshTokenList.containsKey(refresh_token)) {
                    String token = getAccessToken(refresh_token);
                    if (token == null) {
                        return new ResponseEntity<>("refresh_token is wrong", HttpStatus.UNAUTHORIZED);
                    }
                    refreshTokenList.put(refresh_token, token);
                    log.info("refreshTokenList初始化成功！");
                }
                String access_token = refreshTokenList.get(refresh_token);
                Map<String, String> headersMap = new HashMap<>();
                //添加头部
                addHeader(headersMap, access_token);
                String json = com.alibaba.fastjson2.JSON.toJSONString(conversation);
                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(json, JSON);
                log.info("请求回复接口："+chatUrl);
                Request.Builder requestBuilder = new Request.Builder().url(chatUrl).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    log.info(resp.toString());
                    if (!resp.isSuccessful()) {
                        if (resp.code() == 429) {
                            return new ResponseEntity<>("rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS);
                        }else if(resp.code() == 401){
                            return new ResponseEntity<>("models is not exist", HttpStatus.BAD_REQUEST);
                        }
                        else if(resp.code() == 404){
                            return new ResponseEntity<>("404", HttpStatus.NOT_FOUND);
                        }
                        else {
                            String token = getAccessToken(refresh_token);
                            if (token == null) {
                                return new ResponseEntity<>("refresh_token is wrong", HttpStatus.UNAUTHORIZED);
                            }
                            refreshTokenList.put(refresh_token, token);
                            log.info("assess_token过期，refreshTokenList重置化成功！");
                            againConversation(response, conversation, token);
                        }
                    } else {
                        // 流式和非流式输出
                        outPutChat(response, resp, conversation);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, executor).orTimeout(6, TimeUnit.MINUTES).exceptionally(ex -> {
            // 处理超时或其他异常
            if (ex instanceof TimeoutException) {
                return new ResponseEntity<>("Request timed out", HttpStatus.REQUEST_TIMEOUT);
            } else {
                return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }


    /**
     * 如发现token过期
     * 重新回复问题
     *
     * @param response
     * @param conversation
     * @param access_token
     * @return
     */
    public Object againConversation(HttpServletResponse response, @org.springframework.web.bind.annotation.RequestBody Conversation conversation, String access_token) {
        try {
            Map<String, String> headersMap = new HashMap<>();
            //添加头部
            addHeader(headersMap, access_token);
            String json = JSON.toJSONString(conversation);
            // 创建一个 RequestBody 对象
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(json, JSON);
            Request streamRequest = new Request.Builder()
                    .url(chatUrl)
                    .post(requestBody )
                    .addHeader("Authorization", "Bearer " + access_token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            try (Response resp = client.newCall(streamRequest).execute()) {
                if (!resp.isSuccessful()) {
                    return new ResponseEntity<>("refresh_token is wrong Or your network is wrong", HttpStatus.UNAUTHORIZED);
                }
                else {
                    // 流式和非流式输出
                    outPutChat(response, resp, conversation);
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 用于refresh_token 拿到access_token
     *
     * @param refresh_token
     * @return
     * @throws IOException
     */
    private String getAccessToken(String refresh_token) {
        try {
            log.info("将通过这个网址请求access_token：" + getAccessTokenUrl);
            Request request = new Request.Builder()
                    .url(getAccessTokenUrl)
                    .addHeader("Authorization", "Bearer " + refresh_token)
                    .post(RequestBody.create(null, new byte[0]))
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.error("Request failed: " + response.body().string().trim());
                return null;
            }
            String responseContent = response.body().string();
            String access_Token = null;
            try {
                JSONObject jsonResponse = new JSONObject(responseContent);
                access_Token = jsonResponse.getString("access_token");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (response.code() == 200 && access_Token != null && access_Token.startsWith("eyJhb")) {
                return access_Token;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * chat接口的输出
     *
     * @param response
     * @param resp
     * @param conversation
     */
    private void outPutChat(HttpServletResponse response, Response resp, Conversation conversation) {
        try {
            Boolean isStream = conversation.getStream();
            if (isStream != null && isStream) {
                response.setContentType("text/event-stream; charset=UTF-8");
            } else {
                response.setContentType("application/json; charset=utf-8");
            }
            OutputStream out = new BufferedOutputStream(response.getOutputStream());
            InputStream in = new BufferedInputStream(resp.body().byteStream());
            // 一次拿多少数据 迭代循环
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addHeader(Map<String, String> headersMap, String access_token) {
        headersMap.put("Accept-Encoding", "gzip, deflate, br");
        headersMap.put("Accept", "*/*");
        headersMap.put("Authorization", "Bearer " + access_token);
        headersMap.put("User-Agent","PostmanRuntime/7.36.1");
    }
}