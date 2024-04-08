package com.refresh.gptChat.controller;

import com.refresh.gptChat.pojo.Conversation;
import com.refresh.gptChat.pojo.Image;
import com.refresh.gptChat.pojo.Result;
import com.refresh.gptChat.pojo.Speech;
import com.refresh.gptChat.service.messageService;
import com.refresh.gptChat.service.outPutService;
import com.refresh.gptChat.service.processService;
import com.refresh.gptChat.service.tokenService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yangyang
 * @create 2023-12-25 18:29
 */

@Slf4j
@RestController()
public class chatController {
    /**
     * 最大上传音频文件 4MB
     */
    public static final long MAX_FILE_SIZE = 4 * 1024 * 1024;
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
    private static final String audioPath = "/v1/audio/transcriptions";
    /**
     * speech接口
     */
    private static final String speechPath = "/v1/audio/speech";
    /**
     * utf-8类型
     */
    private static final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
    /**
     * 定义正则
     */
    private static final Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
    /**
     * 缓存access_token
     */
    private static ConcurrentHashMap<String, String> refreshTokenList;
    /**
     * 定义线程池里的线程名字
     */
    private static ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "refreshPool-" + counter.getAndIncrement());
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
    private final OkHttpClient client = new OkHttpClient.Builder().
            connectTimeout(3, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES).
            writeTimeout(5, TimeUnit.MINUTES).build();
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
     * 画图使用的模型 gpt-4-mobile 或者 gpt-4
     */
    @Value("${image_mobel}")
    private String image_mobel;
    /**
     * 导入outPutService/messageService/tokenService
     */
    @Autowired
    private outPutService outPutService;
    @Autowired
    private messageService messageService;
    @Autowired
    private tokenService tokenService;
    @Autowired
    private processService processService;

    public static ConcurrentHashMap<String, String> getRefreshTokenList() {
        return refreshTokenList;
    }

    public static void setExecutor(Integer maxPoolSize) {
        executor = new ThreadPoolExecutor(0, maxPoolSize, 60L,
                TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
    }

    @PostConstruct
    public void init() {
        setExecutor(max_threads);
    }

    @Scheduled(cron = "0 0 0 */3 * ?")
    private void clearModelsUsage() {
        int count = 0;
        for (Map.Entry<String, String> entry : refreshTokenList.entrySet()) {
            String access_token = tokenService.getAccessToken(entry.getKey());
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
                String[] result = messageService.extractApiKeyAndRequestUrl(authorizationHeader);
                String refresh_token = result[0];
                String request_url = result[1];
                String request_id = result[2];
                String access_token = getAccess_token(refresh_token);
                Map<String, String> headersMap = tokenService.addHeader(access_token, request_id);
                String json = com.alibaba.fastjson2.JSON.toJSONString(conversation);
                RequestBody requestBody = RequestBody.create(json, mediaType);
                String chatUrl = request_url;
                if (request_url.contains(chatPath)) {
                    chatUrl = request_url.replace(chatPath, "");
                }
                chatUrl = chatUrl + chatPath;
                log.info("请求回复接口：" + chatUrl);
                Request.Builder requestBuilder = new Request.Builder().url(chatUrl).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    if (!resp.isSuccessful()) {
                        processService.chatManageUnsuccessfulResponse(refreshTokenList, resp,
                                refresh_token, response, conversation, chatUrl,
                                request_id);
                    } else {
                        // 流式和非流式输出
                        outPutService.outPutChat(response, resp, conversation);
                    }
                } catch (ResponseStatusException e) {
                    return new ResponseEntity<>(e.getMessage(), e.getStatus());
                } catch (Exception e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
                }
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
            return null;
        }, executor);
        return outPutService.getObjectResponseEntity(response, future);
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
    public ResponseEntity<Object> imageConversation(HttpServletResponse response,
                                                    HttpServletRequest request,
                                                    @org.springframework.web.bind.annotation.RequestBody Image conversation) {
        String header = request.getHeader("Authorization");
        String authorizationHeader = (header != null && !header.trim().isEmpty()) ? header.trim() : null;
        // 异步处理
        CompletableFuture<ResponseEntity<Object>> future = CompletableFuture.supplyAsync(() -> {
            try {
                String[] result = messageService.extractApiKeyAndRequestUrl(authorizationHeader);
                String refresh_token = result[0];
                String request_url = result[1];
                String request_id = result[2];
                String access_token = getAccess_token(refresh_token);
                Map<String, String> headersMap = tokenService.addHeader(access_token, request_id);
                String imageUrl = request_url;
                // 检查URL是否包含要去除的部分
                if (request_url.contains(chatPath)) {
                    imageUrl = request_url.replace(chatPath, "");
                }
                if (!imageUrl.contains("oaifree")) {
                    String json = com.alibaba.fastjson2.JSON.toJSONString(conversation);
                    RequestBody requestBody = RequestBody.create(json, mediaType);
                    // 去除指定部分
                    imageUrl = request_url + imagePath;
                    log.info("请求image回复接口：" + imageUrl);
                    Request.Builder requestBuilder = new Request.Builder().url(imageUrl).post(requestBody);
                    headersMap.forEach(requestBuilder::addHeader);
                    Request streamRequest = requestBuilder.build();
                    try (Response resp = client.newCall(streamRequest).execute()) {
                        if (!resp.isSuccessful()) {
                            processService.imageManageUnsuccessfulResponse(refreshTokenList, resp,
                                    refresh_token, response, conversation, imageUrl,
                                    request_id);
                        } else {
                            // 回复image回答
                            outPutService.outPutImage(response, resp, conversation);
                        }
                    } catch (ResponseStatusException e) {
                        return new ResponseEntity<>(e.getMessage(), e.getStatus());
                    } catch (Exception e) {
                        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
                    }
                } else {
                    String json = "{\n" +
                            "  \"model\": \"" + (image_mobel != null ? image_mobel : "gpt-4") + "\",\n" +
                            "  \"stream\": false,\n" +
                            "  \"messages\": [\n" +
                            "    {\n" +
                            "      \"content\": \"" + conversation.getPrompt() + "\",\n" +
                            "      \"role\": \"user\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}";
                    RequestBody requestBody = RequestBody.create(json, mediaType);
                    // 去除指定部分
                    imageUrl = request_url.replace(chatPath, "") + chatPath;
                    log.info("请求image回复接口：" + imageUrl);
                    Request.Builder requestBuilder = new Request.Builder().url(imageUrl).post(requestBody);
                    headersMap.forEach(requestBuilder::addHeader);
                    Request streamRequest = requestBuilder.build();
                    try (Response resp = client.newCall(streamRequest).execute()) {
                        if (!resp.isSuccessful()) {
                            processService.imageManageUnsuccessfulResponse(refreshTokenList, resp,
                                    refresh_token, response, conversation, imageUrl,
                                    request_id);
                        } else {
                            String respStr = resp.body().string();
                            JSONObject jsonObject = new JSONObject(respStr);
                            String created = jsonObject.getString("created");
                            JSONArray choicesArray = jsonObject.getJSONArray("choices");
                            if (choicesArray.length() > 0) {
                                JSONObject firstChoice = choicesArray.getJSONObject(0);
                                JSONObject messageObject = firstChoice.getJSONObject("message");
                                String content = messageObject.getString("content");
                                Matcher matcher = pattern.matcher(content);
                                if (matcher.find()) {
                                    String urlAndText = matcher.group(1);
                                    String[] splitArray = urlAndText.split(" ", 2);
                                    if (splitArray.length == 2) {
                                        String url = splitArray[0].trim();
                                        String reply = "```\n{ " + splitArray[1].trim() + "}\n```";
                                        JSONObject dataObject = new JSONObject();
                                        dataObject.put("url", url);
                                        JSONObject newJson = new JSONObject();
                                        newJson.put("created", created);
                                        newJson.put("data", dataObject);
                                        newJson.put("reply", reply);
                                        outPutService.outPutOaifreeImage(response, newJson, conversation);
                                    }
                                }
                            } else {
                                return new ResponseEntity<>(Result.error("INTERNAL SERVER ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }

                    } catch (ResponseStatusException e) {
                        return new ResponseEntity<>(e.getMessage(), e.getStatus());
                    } catch (Exception e) {
                        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
                    }
                }

            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(Result.error(e.getMessage()), HttpStatus.BAD_REQUEST);
            }
            return null;
        }, executor);
        return outPutService.getObjectResponseEntity(response, future);
    }


    /**
     * 自定义speech接口
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
    @PostMapping(value = "/v1/audio/speech")
    public ResponseEntity<Object> audioConversation(HttpServletResponse response,
                                                    HttpServletRequest request,
                                                    @org.springframework.web.bind.annotation.RequestBody Speech conversation) {
        String header = request.getHeader("Authorization");
        String authorizationHeader = (header != null && !header.trim().isEmpty()) ? header.trim() : null;
        // 异步处理
        CompletableFuture<ResponseEntity<Object>> future = CompletableFuture.supplyAsync(() -> {
            try {
                if (conversation == null) {
                    return new ResponseEntity<>(Result.error("Request body is missing or not in JSON format"), HttpStatus.BAD_REQUEST);
                }
                String[] result = messageService.extractApiKeyAndRequestUrl(authorizationHeader);
                String refresh_token = result[0];
                String request_url = result[1];
                String request_id = result[2];
                String access_token = getAccess_token(refresh_token);
                String speechUrl = request_url;
                // 检查URL是否包含要去除的部分
                if (request_url.contains(chatPath)) {
                    speechUrl = request_url.replace(chatPath, "") + speechPath;
                }
                speechUrl = speechUrl + speechPath;
                Map<String, String> headersMap = tokenService.addHeader(access_token, request_id);
                String json = com.alibaba.fastjson2.JSON.toJSONString(conversation);
                RequestBody requestBody = RequestBody.create(json, mediaType);
                log.info("请求speech回复接口：" + speechUrl);
                Request.Builder requestBuilder = new Request.Builder().url(speechUrl).post(requestBody);
                headersMap.forEach(requestBuilder::addHeader);
                Request streamRequest = requestBuilder.build();
                try (Response resp = client.newCall(streamRequest).execute()) {
                    if (!resp.isSuccessful()) {
                        processService.speechManageUnsuccessfulResponse(refreshTokenList, resp,
                                refresh_token, response, conversation, speechUrl,
                                request_id);
                    } else {
                        // speech 输出
                        outPutService.outPutSpeech(response, resp, conversation);
                    }
                } catch (ResponseStatusException e) {
                    return new ResponseEntity<>(e.getMessage(), e.getStatus());
                } catch (Exception e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
                }
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
            return null;
        }, executor);
        return outPutService.getObjectResponseEntity(response, future);
    }


    /**
     * 自定义audio接口
     * 请求体不是json 会报Request body is missing or not in JSON format
     * Authorization token缺失  会报Authorization header is missing
     * 无法请求到access_token 会报refresh_token is wrong
     *
     * @param response
     * @param request
     * @return
     * @throws JSONException
     * @throws IOException
     */
    @PostMapping(value = "/v1/audio/transcriptions")
    public ResponseEntity<Object> AudioConversation(HttpServletResponse response,
                                                    HttpServletRequest request,
                                                    @RequestPart("file") MultipartFile file,
                                                    @RequestPart("model") String model) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("Missing file", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return new ResponseEntity<>("File is too large, limit is: " + MAX_FILE_SIZE, HttpStatus.BAD_REQUEST);
        }
        String filename = file.getOriginalFilename();
        log.info("上传文件名：" + filename + "\n上传文件名：" + file.getSize());
        log.info("上传模型：" + model);
        if (model == null || model.trim().isEmpty()) {
            return new ResponseEntity<>("Model cannot be empty", HttpStatus.BAD_REQUEST);
        }
        String header = request.getHeader("Authorization");
        String authorizationHeader = (header != null && !header.trim().isEmpty()) ? header.trim() : null;
        CompletableFuture<ResponseEntity<Object>> future =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        String[] result = messageService.extractApiKeyAndRequestUrl(authorizationHeader);
                        String refresh_token = result[0];
                        String request_url = result[1];
                        String request_id = result[2];
                        String access_token = getAccess_token(refresh_token);
                        String audioUrl = request_url;
                        if (request_url.contains(chatPath)) {
                            audioUrl = request_url.replace(chatPath, "");
                        }
                        audioUrl = audioUrl + audioPath;
                        Map<String, String> headersMap = tokenService.addHeader(access_token, request_id);
                        RequestBody fileBody = RequestBody.create(file.getBytes(),
                                MediaType.parse("application/octet-stream"));
                        log.info("请求speech回复接口：" + audioUrl);
                        RequestBody body = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("model", model)
                                .addFormDataPart("file", filename, fileBody)
                                .build();
                        Request.Builder requestBuilder = new Request.Builder()
                                .url(audioUrl)
                                .post(body);
                        headersMap.forEach(requestBuilder::addHeader);
                        try (Response resp = client.newCall(requestBuilder.build()).execute()) {
                            log.info(resp.toString());
                            if (!resp.isSuccessful()) {
                                processService.audioManageUnsuccessfulResponse(refreshTokenList, resp,
                                        refresh_token, response, fileBody, filename, model,
                                        audioUrl, request_id);
                            } else {
                                outPutService.outPutAudio(response, resp, model);
                            }
                        }
                    } catch (Exception e) {
                        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
                    }
                    return null;
                }, executor);
        return outPutService.getObjectResponseEntity(response, future);
    }

    private String getAccess_token(String refresh_token) {
        String access_token = refresh_token;
        boolean is_access = refresh_token.startsWith("eyJhb");
        if (!refreshTokenList.containsKey(refresh_token) && !is_access) {
            String token = tokenService.getAccessToken(refresh_token);
            if (token == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh_token is wrong!");
            }
            refreshTokenList.put(refresh_token, token);
            log.info("refreshTokenList初始化成功！");
            access_token = refreshTokenList.get(refresh_token);
        } else {
            if (!is_access) {
                log.info("从缓存读取access_token成功！");
                access_token = refreshTokenList.get(refresh_token);
            }
        }
        return access_token;
    }
}