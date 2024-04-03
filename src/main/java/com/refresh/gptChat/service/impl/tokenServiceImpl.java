package com.refresh.gptChat.service.impl;

import com.refresh.gptChat.service.tokenService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Yangyang
 * @create 2024-04-03 17:10
 */

@Slf4j
@Service
public class tokenServiceImpl implements tokenService {
    /**
     * utf-8类型
     */
    private static final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
    /**
     * xyhelper 获取 access_token 接口
     */
    private static final String getAccessTokenUrl_xyhelper = "https://demo.xyhelper.cn/applelogin";
    /**
     * oai 获取 access_token 接口
     */
    private static final String getAccessTokenUrl_oai = "https://auth0.openai.com/oauth/token";
    /**
     * oaiFree 获取 access_token 接口
     */
    private static final String getAccessTokenUrl_oaiFree = "https://token.oaifree.com/api/auth/refresh";
    /**
     * okhttp3 client服务定义
     */
    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.MINUTES).readTimeout(2, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).build();
    /**
     * 获取 access_token 服务名称 oai/xyhelper/ninja
     */
    @Value("${getAccessTokenService}")
    private String getAccessTokenService;
    /**
     * ninja 获取 access_token 接口
     */
    @Value("${getAccessTokenUrl_ninja}")
    private String getAccessTokenUrl_ninja;

    /**
     * 用于refresh_token 拿到 access_token
     *
     * @param refresh_token
     * @return
     * @throws IOException
     */
    public String getAccessToken(String refresh_token) {
        if ("oai".equals(getAccessTokenService.toLowerCase())) {
            return oaiGetAccessToken(refresh_token);
        } else if ("ninja".equals(getAccessTokenService.toLowerCase())) {
            return ninjaGetAccessToken(refresh_token);
        } else if ("oaifree".equals(getAccessTokenService.toLowerCase())) {
            return oaiFreeGetAccessToken(refresh_token);
        } else {
            return xyhelperGetAccessToken(refresh_token);
        }
    }

    private String xyhelperGetAccessToken(String refreshToken) {
        try {
            log.info("将通过这个网址请求access_token：" + getAccessTokenUrl_xyhelper);
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

    private String oaiFreeGetAccessToken(String refresh_token) {
        try {
            log.info("将通过这个网址请求access_token：" + getAccessTokenUrl_oaiFree);
            RequestBody body = new FormBody.Builder()
                    .add("refresh_token", refresh_token)
                    .build();
            Request request = new Request.Builder()
                    .url(getAccessTokenUrl_oaiFree)
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .post(body)
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
}
