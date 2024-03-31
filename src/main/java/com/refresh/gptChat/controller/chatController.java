package com.refresh.gptChat.controller;

import com.alibaba.fastjson2.JSON;
import com.refresh.gptChat.pojo.Conversation;
import com.refresh.gptChat.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yangyang
 * @create 2023-12-25 18:29
 */

@Slf4j
@RestController()
public class chatController {
    /**
     * 缓存access_token
     */
    private static final ConcurrentHashMap<String, String> refreshTokenList;

    /**
     * image接口
     */
    private static final String imagePath = "/v1/images/generations";

    /**
     * chat接口
     */
    private static final String chatPath = "/v1/chat/completions";

    /**
     * audio接口
     */
    private static final String vedioPath = "v1/audio/transcriptions";

    /**
     * xyhelper 获取 access_token 接口
     */
    private static final String getAccessTokenUrl_xyhelper = "https://demo.xyhelper.cn/applelogin";

    /**
     * oai 获取 access_token 接口
     */
    private static final String getAccessTokenUrl_oai = "https://auth0.openai.com/oauth/token";

    /**
     * utf-8类型
     */
    private static final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
    /**
     * 定义线程池里的线程名字
     */
    private static ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "chatThreadPool-" + counter.getAndIncrement());
        }
    };
    /**
     * 定义线程池
     */
    private static ExecutorService executor;
    /**
     * 删掉模型前缀 gpt-4-gizmo-
     */
    private static String cancelGizmo = "gpt-4-gizmo-";

    static {
        refreshTokenList = new ConcurrentHashMap<>();
    }

    /**
     * okhttp3 client服务定义
     */
    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).build();
    /**
     * 服务端口
     */
    @Value("${server.port}")
    private String serverPort;
    /**
     * 服务后缀
     */
    @Value("${server.servlet.context-path}")
    private String prefix;
    /**
     * 最大线程数
     */
    @Value("${max_threads}")
    private int max_threads;
    /**
     * is cancel gpt-4-gizmo
     */
    @Value("${isCancelGizmo}")
    private boolean isCancelGizmo;
    /**
     * ninja 获取 access_token 接口
     */
    @Value("${getAccessTokenUrl_ninja}")
    private String getAccessTokenUrl_ninja;
    /**
     * 获取 access_token 服务名称 oai/xyhelper/ninja
     */
    @Value("${getAccessTokenService}")
    private String getAccessTokenService;

    public static void setExecutor(Integer maxPoolSize) {
        executor = new ThreadPoolExecutor(0, maxPoolSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
    }

    @PostConstruct
    public void init() {
        setExecutor(max_threads);
    }

    @Scheduled(cron = "0 0 0 */3 * ?")
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
    public ResponseEntity<Object> chatConversation(HttpServletResponse response, HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody Conversation conversation) {
        String header = request.getHeader("Authorization");
        String authorizationHeader = (header != null && !header.trim().isEmpty()) ? header.trim() : null;
        // 异步处理
        CompletableFuture<ResponseEntity<Object>> future = CompletableFuture.supplyAsync(() -> {
            try {
                if (conversation == null) {
                    return new ResponseEntity<>(Result.error("Request body is missing or not in JSON format"), HttpStatus.BAD_REQUEST);
                }
                if (conversation.getModel().startsWith("gpt-4-gizmo") && isCancelGizmo) {
                    conversation.setModel(conversation.getModel().replace(cancelGizmo, ""));
                }
                String[] result = extractApiKeyAndRequestUrl(authorizationHeader, conversation);
                String refresh_token = result[0];
                String request_url = result[1];
                String request_id = result[2];
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
                addHeader(headersMap, access_token, request_id);
                String json = com.alibaba.fastjson2.JSON.toJSONString(conversation);
                RequestBody requestBody = RequestBody.create(json, mediaType);
                log.info("请求回复接口：" + request_url);
                Request.Builder requestBuilder = new Request.Builder().url(request_url).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    if (!resp.isSuccessful()) {
                        if (resp.code() == 429) {
                            return new ResponseEntity<>("rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS);
                        } else if (resp.code() == 401) {
                            return new ResponseEntity<>("models is not exist", HttpStatus.BAD_REQUEST);
                        } else if (resp.code() == 404) {
                            return new ResponseEntity<>("404", HttpStatus.NOT_FOUND);
                        } else if (resp.code() == 500) {
                            return new ResponseEntity<>("INTERNAL SERVER ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
                        } else {
                            String token = getAccessToken(refresh_token);
                            if (token == null) {
                                return new ResponseEntity<>("refresh_token is wrong", HttpStatus.UNAUTHORIZED);
                            }
                            refreshTokenList.put(refresh_token, token);
                            log.info("assess_token过期，refreshTokenList重置化成功！");
                            againChatConversation(response, conversation, token, request_url, request_id);
                        }
                    } else {
                        // 流式和非流式输出
                        outPutChat(response, resp, conversation);
                    }
                }
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, executor);

        return getObjectResponseEntity(response, future);
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
    public Object againChatConversation(HttpServletResponse response,
                                        @org.springframework.web.bind.annotation.RequestBody Conversation conversation,
                                        String access_token,
                                        String chat_url,
                                        String request_id) {
        try {
            Map<String, String> headersMap = new HashMap<>();
            //添加头部
            addHeader(headersMap, access_token, request_id);
            String json = JSON.toJSONString(conversation);
            // 创建一个 RequestBody 对象
            RequestBody requestBody = RequestBody.create(json, mediaType);
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
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 自定义Image接口
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
    public ResponseEntity<Object> imageConversation(HttpServletResponse response, HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody Conversation conversation) {
        String header = request.getHeader("Authorization");
        String authorizationHeader = (header != null && !header.trim().isEmpty()) ? header.trim() : null;
        // 异步处理
        CompletableFuture<ResponseEntity<Object>> future = CompletableFuture.supplyAsync(() -> {
            try {
                String[] result = extractApiKeyAndRequestUrl(authorizationHeader, conversation);
                String refresh_token = result[0];
                String request_url = result[1];
                String request_id = result[2];
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
                addHeader(headersMap, access_token, request_id);
                String json = com.alibaba.fastjson2.JSON.toJSONString(conversation);
                RequestBody requestBody = RequestBody.create(json, mediaType);
                String imageUrl = request_url;
                // 检查URL是否包含要去除的部分
                if (request_url.contains(chatPath)) {
                    // 去除指定部分
                    imageUrl = request_url.replace(chatPath, "") + imagePath;
                }
                log.info("请求image回复接口：" + imageUrl);
                Request.Builder requestBuilder = new Request.Builder().url(imageUrl).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    if (!resp.isSuccessful()) {
                        if (resp.code() == 429) {
                            return new ResponseEntity<>("rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS);
                        } else if (resp.code() == 401) {
                            return new ResponseEntity<>("models is not exist", HttpStatus.BAD_REQUEST);
                        } else if (resp.code() == 404) {
                            return new ResponseEntity<>("404", HttpStatus.NOT_FOUND);
                        } else if (resp.code() == 500) {
                            return new ResponseEntity<>("INTERNAL SERVER ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
                        } else {
                            String token = getAccessToken(refresh_token);
                            if (token == null) {
                                return new ResponseEntity<>("refresh_token is wrong", HttpStatus.UNAUTHORIZED);
                            }
                            refreshTokenList.put(refresh_token, token);
                            log.info("assess_token过期，refreshTokenList重置化成功！");
                            againImageConversation(response, conversation, token, imageUrl, request_id);
                        }
                    } else {
                        // 回复image回答
                        outPutImage(response, resp, conversation);
                    }
                }

            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, executor);
        return getObjectResponseEntity(response, future);
    }

    /**
     * 返回异步responseEntity
     *
     * @param response future
     */
    private ResponseEntity<Object> getObjectResponseEntity(HttpServletResponse response, CompletableFuture<ResponseEntity<Object>> future) {
        ResponseEntity<Object> responseEntity;

        try {
            responseEntity = future.get(6, TimeUnit.MINUTES);
        } catch (TimeoutException ex) {
            response.setContentType("application/json; charset=utf-8");
            future.cancel(true);
            responseEntity = new ResponseEntity<>(Result.error("The Chat timed out"), HttpStatus.REQUEST_TIMEOUT);
        } catch (Exception ex) {
            response.setContentType("application/json; charset=utf-8");
            log.error(ex.getMessage());
            responseEntity = new ResponseEntity<>(Result.error("An error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return responseEntity;
    }

    /**
     * 获取url和apiKey
     *
     * @param authorizationHeader
     * @param conversation
     * @throws IOException
     */
    private String[] extractApiKeyAndRequestUrl(String authorizationHeader, Conversation conversation) throws IllegalArgumentException {
        if (conversation == null || authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid input");
        }
        String[] tempResult = Arrays.stream(authorizationHeader.substring(7).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        if (tempResult.length < 2) {
            throw new IllegalArgumentException("Authorization ApiKey and requestUrl is missing, Please read the documentation!");
        }
        String[] finalResult = new String[3];
        // 复制解析后的值到finalResult数组，不足3个的部分将保持为null
        System.arraycopy(tempResult, 0, finalResult, 0, Math.min(tempResult.length, 3));
        return finalResult;
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
    public Object againImageConversation(HttpServletResponse response,
                                         @org.springframework.web.bind.annotation.RequestBody Conversation conversation,
                                         String access_token,
                                         String chat_url,
                                         String request_id) {
        try {
            Map<String, String> headersMap = new HashMap<>();
            //添加头部
            addHeader(headersMap, access_token, request_id);
            String json = JSON.toJSONString(conversation);
            // 创建一个 RequestBody 对象
            RequestBody requestBody = RequestBody.create(json, mediaType);
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
                    outPutImage(response, resp, conversation);
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
        if ("oai".equals(getAccessTokenService.toLowerCase())) {
            return oaiGetAccessToken(refresh_token);
        } else if ("ninja".equals(getAccessTokenService.toLowerCase())) {
            return ninjaGetAccessToken(refresh_token);
        } else {
            return xyhelperGetAccessToken(refresh_token);
        }
    }

    private String xyhelperGetAccessToken(String refreshToken) {
        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("refresh_token", refreshToken)
                    .build();
            Request request = new Request.Builder()
                    .url(getAccessTokenUrl_xyhelper)
                    .post(formBody)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Request failed: " + response.body().string().trim());
                    return null;
                }
                String responseContent = response.body().string();
                String access_Token;
                JSONObject jsonResponse = new JSONObject(responseContent);
                access_Token = jsonResponse.getString("access_token");
                if (response.code() == 200 && access_Token != null && access_Token.startsWith("eyJhb")) {
                    return access_Token;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String oaiGetAccessToken(String refresh_token) {
        try {
            log.info("将通过这个网址请求access_token：" + getAccessTokenUrl_oai);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("redirect_uri", "com.openai.chat://auth0.openai.com/ios/com.openai.chat/callback");
            jsonObject.put("grant_type", "refresh_token");
            jsonObject.put("client_id", "pdlLIX2Y72MIl2rhLhTE9VV9bN905kBh");
            jsonObject.put("refresh_token", refresh_token);
            RequestBody body = RequestBody.create(jsonObject.toString(), mediaType);
            Request request = new Request.Builder()
                    .url(getAccessTokenUrl_oai)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Request failed: " + response.body().string().trim());
                    return null;
                }
                String responseContent = response.body().string();
                String access_Token;
                JSONObject jsonResponse = new JSONObject(responseContent);
                access_Token = jsonResponse.getString("access_token");
                if (response.code() == 200 && access_Token != null && access_Token.startsWith("eyJhb")) {
                    return access_Token;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String ninjaGetAccessToken(String refresh_token) {
        try {
            log.info("将通过这个网址请求access_token：" + getAccessTokenUrl_ninja);
            RequestBody emptyBody = RequestBody.create("", mediaType);
            Request request = new Request.Builder()
                    .url(getAccessTokenUrl_ninja)
                    .addHeader("Authorization", "Bearer " + refresh_token)
                    .post(emptyBody)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Request failed: " + response.body().string().trim());
                    return null;
                }
                String responseContent = response.body().string();
                String access_Token = null;
                JSONObject jsonResponse = new JSONObject(responseContent);
                access_Token = jsonResponse.getString("access_token");
                if (response.code() == 200 && access_Token != null && access_Token.startsWith("eyJhb")) {
                    return access_Token;
                }
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
            JSONObject jsonObject = new JSONObject(com.alibaba.fastjson2.JSON.toJSONString(conversation));
            String model = (conversation.getModel() != null) ? conversation.getModel() : "gpt-3.5-turbo";
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
                    if (sleep_time > 0) {
                        Thread.sleep(sleep_time);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.info("使用模型：" + model + "，" + resp);
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
    private void outPutImage(HttpServletResponse response, Response resp, Conversation conversation) {
        try {
            response.setContentType("application/json; charset=utf-8");
            String model = (conversation.getModel() != null) ? conversation.getModel() : "gpt-3.5-turbo";
            OutputStream out = new BufferedOutputStream(response.getOutputStream());
            InputStream in = new BufferedInputStream(resp.body().byteStream());
            // 一次拿多少数据 迭代循环
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
            log.info("使用模型：" + model + "，" + resp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addHeader(Map<String, String> headersMap, String access_token, String request_id) {
        headersMap.put("Authorization", "Bearer " + access_token);
        if (request_id != null) {
            headersMap.put("id", request_id);
        }
    }
}