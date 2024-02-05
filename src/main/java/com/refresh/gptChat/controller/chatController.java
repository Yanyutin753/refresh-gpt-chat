package com.refresh.gptChat.controller;

import com.alibaba.fastjson2.JSON;
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
    @Value("${ninja_chatUrl}")
    private String ninja_chatUrl;

    private static final String imagePath = "/v1/images/generations";

    private static final String chatPath = "/v1/chat/completions";

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
     * 自定义chat接口
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
    public CompletableFuture<ResponseEntity<String>> chatConversation(HttpServletResponse response, HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody Object conversation) {
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
                log.info("请求回复接口：" + chatUrl);
                Request.Builder requestBuilder = new Request.Builder().url(chatUrl).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    log.info(resp.toString());
                    if (!resp.isSuccessful()) {
                        if (resp.code() == 429) {
                            return new ResponseEntity<>("rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS);
                        } else if (resp.code() == 401) {
                            return new ResponseEntity<>("models is not exist", HttpStatus.BAD_REQUEST);
                        } else if (resp.code() == 404) {
                            return new ResponseEntity<>("404", HttpStatus.NOT_FOUND);
                        } else if (resp.code() == 500) {
                            return new ResponseEntity<>("INTERNAL SERVER ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                        else {
                            String token = getAccessToken(refresh_token);
                            if (token == null) {
                                return new ResponseEntity<>("refresh_token is wrong", HttpStatus.UNAUTHORIZED);
                            }
                            refreshTokenList.put(refresh_token, token);
                            log.info("assess_token过期，refreshTokenList重置化成功！");
                            againChatConversation(response, conversation, token, chatUrl);
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
     * ninja_chat接口
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
    @PostMapping(value = "ninja/v1/chat/completions")
    public CompletableFuture<ResponseEntity<String>> ninjaConversation(HttpServletResponse response, HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody Object conversation) {
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
                log.info("请求回复接口：" + ninja_chatUrl);
                Request.Builder requestBuilder = new Request.Builder().url(ninja_chatUrl).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    log.info(resp.toString());
                    if (!resp.isSuccessful()) {
                        if (resp.code() == 429) {
                            return new ResponseEntity<>("rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS);
                        } else if (resp.code() == 401) {
                            return new ResponseEntity<>("models is not exist", HttpStatus.BAD_REQUEST);
                        } else if (resp.code() == 404) {
                            return new ResponseEntity<>("404", HttpStatus.NOT_FOUND);
                        } else {
                            String token = getAccessToken(refresh_token);
                            if (token == null) {
                                return new ResponseEntity<>("refresh_token is wrong", HttpStatus.UNAUTHORIZED);
                            }
                            refreshTokenList.put(refresh_token, token);
                            log.info("assess_token过期，refreshTokenList重置化成功！");
                            againChatConversation(response, conversation, token, chatUrl);
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
     * /v1/chat/completions
     * 如发现token过期
     * 重新回复问题
     *
     * @param response
     * @param conversation
     * @param access_token
     * @return
     */
    public Object againChatConversation(HttpServletResponse response, @org.springframework.web.bind.annotation.RequestBody Object conversation, String access_token, String chat_url) {
        try {
            Map<String, String> headersMap = new HashMap<>();
            //添加头部
            addHeader(headersMap, access_token);
            String json = JSON.toJSONString(conversation);
            // 创建一个 RequestBody 对象
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(json, JSON);
            Request streamRequest = new Request.Builder()
                    .url(chat_url)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + access_token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            try (Response resp = client.newCall(streamRequest).execute()) {
                if (!resp.isSuccessful()) {
                    return new ResponseEntity<>("refresh_token is wrong Or your network is wrong", HttpStatus.UNAUTHORIZED);
                } else {
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
     * 自定义chat接口
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
    @PostMapping(value = "/v1/images/generations")
    public CompletableFuture<ResponseEntity<String>> imageConversation(HttpServletResponse response, HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody Object conversation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String imageUrl = null;
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
                // 检查URL是否包含要去除的部分
                if (chatUrl.contains(chatPath)){
                    // 去除指定部分
                    imageUrl = chatUrl.replace(chatPath, "") + imagePath;
                }
                log.info("请求image回复接口：" + imageUrl);
                Request.Builder requestBuilder = new Request.Builder().url(imageUrl).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    log.info(resp.toString());
                    if (!resp.isSuccessful()) {
                        if (resp.code() == 429) {
                            return new ResponseEntity<>("rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS);
                        } else if (resp.code() == 401) {
                            return new ResponseEntity<>("models is not exist", HttpStatus.BAD_REQUEST);
                        } else if (resp.code() == 404) {
                            return new ResponseEntity<>("404", HttpStatus.NOT_FOUND);
                        } else if (resp.code() == 500) {
                            return new ResponseEntity<>("INTERNAL SERVER ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                        else {
                            String token = getAccessToken(refresh_token);
                            if (token == null) {
                                return new ResponseEntity<>("refresh_token is wrong", HttpStatus.UNAUTHORIZED);
                            }
                            refreshTokenList.put(refresh_token, token);
                            log.info("assess_token过期，refreshTokenList重置化成功！");
                            againImageConversation(response, conversation, token, imageUrl);
                        }
                    } else {
                        // 回复image回答
                        outPutImage(response, resp);
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
     * ninja_image接口
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
    @PostMapping(value = "/ninja/v1/images/generations")
    public CompletableFuture<ResponseEntity<String>> ninjaImageConversation(HttpServletResponse response, HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody Object conversation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String imageUrl = null;
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
                // 检查URL是否包含要去除的部分
                if (ninja_chatUrl.contains(chatPath)){
                    // 去除指定部分
                    imageUrl = ninja_chatUrl.replace(chatPath, "") + imagePath;
                }
                log.info("请求image回复接口：" + imageUrl);
                Request.Builder requestBuilder = new Request.Builder().url(imageUrl).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    log.info(resp.toString());
                    if (!resp.isSuccessful()) {
                        if (resp.code() == 429) {
                            return new ResponseEntity<>("rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS);
                        } else if (resp.code() == 401) {
                            return new ResponseEntity<>("models is not exist", HttpStatus.BAD_REQUEST);
                        } else if (resp.code() == 404) {
                            return new ResponseEntity<>("404", HttpStatus.NOT_FOUND);
                        } else {
                            String token = getAccessToken(refresh_token);
                            if (token == null) {
                                return new ResponseEntity<>("refresh_token is wrong", HttpStatus.UNAUTHORIZED);
                            }
                            refreshTokenList.put(refresh_token, token);
                            log.info("assess_token过期，refreshTokenList重置化成功！");
                            againImageConversation(response, conversation, token, imageUrl);
                        }
                    } else {
                        // 回复image回答
                        outPutImage(response, resp);
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
     * /v1/images/generations
     * 如发现token过期
     * 重新回复问题
     *
     * @param response
     * @param conversation
     * @param access_token
     * @return
     */
    public Object againImageConversation(HttpServletResponse response, @org.springframework.web.bind.annotation.RequestBody Object conversation, String access_token, String chat_url) {
        try {
            Map<String, String> headersMap = new HashMap<>();
            //添加头部
            addHeader(headersMap, access_token);
            String json = JSON.toJSONString(conversation);
            // 创建一个 RequestBody 对象
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(json, JSON);
            Request streamRequest = new Request.Builder()
                    .url(chat_url)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + access_token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            try (Response resp = client.newCall(streamRequest).execute()) {
                if (!resp.isSuccessful()) {
                    return new ResponseEntity<>("refresh_token is wrong Or your network is wrong", HttpStatus.UNAUTHORIZED);
                } else {
                    // 回复image回答
                    outPutImage(response, resp);
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
    private void outPutChat(HttpServletResponse response, Response resp, Object conversation) {
        try {
            JSONObject jsonObject = new JSONObject(com.alibaba.fastjson2.JSON.toJSONString(conversation));
            boolean isStream = jsonObject.optBoolean("stream", false);
            int one_messageByte;
            int sleep_time;
            if (isStream) {
                if (!jsonObject.optString("model", "gpt-3.5-turbo").contains("gpt-4")) {
                    one_messageByte = 2048;
                    sleep_time = 0;
                } else {
                    one_messageByte = 128;
                    sleep_time = 25;
                }
                response.setContentType("text/event-stream; charset=UTF-8");
            } else {
                one_messageByte = 8192;
                sleep_time = 0;
                response.setContentType("application/json; charset=utf-8");
            }
            OutputStream out = new BufferedOutputStream(response.getOutputStream());
            InputStream in = new BufferedInputStream(resp.body().byteStream());
            // 一次拿多少数据 迭代循环
            byte[] buffer = new byte[one_messageByte];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
                try {
                    if(sleep_time > 0){
                        Thread.sleep(sleep_time);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * image接口的输出
     *
     * @param response
     * @param resp
     */
    private void outPutImage(HttpServletResponse response, Response resp) {
        try {
            response.setContentType("application/json; charset=utf-8");
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
        headersMap.put("Authorization", "Bearer " + access_token);
    }
}