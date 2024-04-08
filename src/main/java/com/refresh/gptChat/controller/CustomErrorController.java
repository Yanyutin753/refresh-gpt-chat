package com.refresh.gptChat.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yangyang
 * @create 2024-02-01 22:52
 */
@RestController
public class CustomErrorController implements ErrorController {

    /**
     * 错误路径
     */
    private static final String PATH = "/error";

    /**
     * 重定向到错误页
     *
     * @return
     */
    @RequestMapping(value = PATH)
    public ResponseEntity<String> error() {
        return new ResponseEntity<>("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Document</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <p>Thanks you use refresh-gpt-chat 0.6.0</p>\n" +
                "    <p><a href=\"https://apifox.com/apidoc/shared-4b9a7517-3f80-47a1-84fc-fcf78827a04a\">详细使用文档</a></p>\n" +
                "    <p><a href=\"https://github.com/Yanyutin753/refresh-gpt-chat\">项目地址</a></p>\n" +
                "</body>\n" +
                "</html>\n", HttpStatus.OK);
    }

}