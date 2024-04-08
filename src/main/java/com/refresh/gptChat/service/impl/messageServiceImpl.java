package com.refresh.gptChat.service.impl;

import com.refresh.gptChat.service.messageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Yangyang
 * @create 2024-04-03 17:17
 */

@Slf4j
@Service
public class messageServiceImpl implements messageService {
    /**
     * 获取url和apiKey
     *
     * @param authorizationHeader
     * @throws IOException
     */
    public String[] extractApiKeyAndRequestUrl(String authorizationHeader) throws IllegalArgumentException {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
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

}
