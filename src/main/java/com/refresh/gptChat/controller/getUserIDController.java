package com.refresh.gptChat.controller;

import com.refresh.gptChat.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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
     * 通过 refresh_token
     * 获取用户 ChatGPT-Account-ID
     *
     * @param request
     * @return
     */

    @PostMapping("/getAccountID")
    public ResponseEntity<Result> getUserID(HttpServletRequest request) {
        String token = extractToken(request.getHeader("Authorization"));
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error("Authorization token is wrong"));
        }
        try {
            Request req = new Request.Builder()
                    .url("https://chat.openai.com/backend-api/accounts/check/v4-2023-04-27")
                    .header("Authorization", "Bearer " + token)
                    .build();

            Response response = client.newCall(req).execute();

            if (!response.isSuccessful()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Result.error(response.message()));
            }

            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray accounts = jsonObject.getJSONArray("accounts");
            String orderType = extractOrderTypeFromAccount(accounts);

            response.close();
            if (orderType == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Result.error("No team account found"));
            } else {
                return ResponseEntity.ok(Result.success(orderType));
            }
        } catch (JSONException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(e.getMessage()));
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    private String extractOrderTypeFromAccount(JSONArray accounts) throws JSONException {
        for (int i = 0; i < accounts.length(); i++) {
            String orderType = accounts.getJSONObject(i).getJSONObject("entitlement").getString("subscription_plan");
            if (orderType.contains("team")) {
                return orderType;
            }
        }
        return null;
    }
}
