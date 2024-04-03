package com.refresh.gptChat.controller;

import com.refresh.gptChat.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Yangyang
 * @create 2024-04-03 17:44
 */
@Slf4j
@RestController
public class getUserIDController {
    /**
     * okhttp3 client服务定义
     */
    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).build();
    /**
     * 获取个人的uuid
     */
    @Value("${self_server_uuid}")
    private String self_server_uuid;
    /**
     * 导入outPutService/messageService/tokenService
     */
    @Autowired
    private com.refresh.gptChat.service.tokenService tokenService;

    /**
     * 通过 refresh_token
     * 获取用户 ChatGPT-Account-ID
     *
     * @param request
     * @return
     */
    @PostMapping("/getAccountID")
    public ResponseEntity<Object> getUserID(HttpServletRequest request) {
        String refresh_token = extractToken(request.getHeader("Authorization"));
        String access_token = refresh_token;
        if(! refresh_token.startsWith("eyJhb")){
            access_token = getAccessToken(refresh_token);
            if (access_token == null) {
                return createUnauthorizedResponse("Authorization token is wrong");
            }
        }
        try {
            Response response = sendRequest(access_token);
            if (!response.isSuccessful()) {
                return createUnauthorizedResponse(response.message());
            }

            Map<String, Set<String>> accountIds = extractAccountIds(response.body().string());
            response.close();

            if (accountIds.get("team") == null && accountIds.get("plus") == null){
                return createUnauthorizedResponse("No team and plus account found");
            }
            Map<String, Object> responseBodyMap = new HashMap<>();
            responseBodyMap.put("teamIds", accountIds.get("team"));
            responseBodyMap.put("plusIds", accountIds.get("plus"));

            return ResponseEntity.ok(Result.success(responseBodyMap));
        } catch (IOException | JSONException e) {
            return createInternalErrorResponse(e.getMessage());
        }
    }

    private String getAccessToken(String refreshToken) {
        String token;

        if (!chatController.getRefreshTokenList().containsKey(refreshToken)) {
            token = tokenService.getAccessToken(refreshToken);

            if (token == null) {
                return null;
            }

            chatController.getRefreshTokenList().put(refreshToken, token);
            log.info("refreshTokenList初始化成功！");
        }

        return chatController.getRefreshTokenList().get(refreshToken);
    }

    private Response sendRequest(String accessToken) throws IOException {
        Request req = new Request.Builder()
                .url("https://chat.oaifree.com/" + self_server_uuid + "/backend-api/accounts/check/v4-2023-04-27")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        return client.newCall(req).execute();
    }

    private Map<String, Set<String>> extractAccountIds(String response) throws JSONException {
        HashMap<String, Set<String>> accountIds = new HashMap<>();
        log.info("response: {}", response);
        JSONObject jsonObj = new JSONObject(response);
        JSONObject accounts = jsonObj.getJSONObject("accounts");
        Set<String> teamIds = new HashSet<>();
        Set<String> plusIds = new HashSet<>();
        Iterator<String> keys = accounts.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject specificAccount = accounts.getJSONObject(key);
            JSONObject account = specificAccount.getJSONObject("account");
            String planType = account.getString("plan_type");
            String account_id = account.getString("account_id");
            if (planType.equals("team")) {
                teamIds.add(account_id);
                accountIds.put("team", teamIds);
            }
            else if (planType.equals("plus")){
                plusIds.add(account_id);
                accountIds.put("plus", plusIds);
            }
        }
        return accountIds;
    }

    private ResponseEntity<Object> createUnauthorizedResponse(String message) {
        return new ResponseEntity<>(Result.error(message), HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Object> createInternalErrorResponse(String message) {
        return new ResponseEntity<>(Result.error(message), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}