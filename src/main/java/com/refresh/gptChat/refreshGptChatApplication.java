package com.refresh.gptChat;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

/**
 * @author YANGYANG
 */

/**
 * 定时注解
 */
@Slf4j
@EnableScheduling
@SpringBootApplication
public class refreshGptChatApplication {

    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Value("${server.port}")
    private String serverPort;
    @Value("${getAccessTokenUrl}")
    private String getAccessTokenUrl;
    @Value("${chatUrl}")
    private String chatUrl;
    @Value("${ninja_chatUrl}")
    private String ninja_chatUrl;
    @Value("${enableOai}")
    private boolean enableOai;

    public static void main(String[] args) {
        SpringApplication.run(refreshGptChatApplication.class, args);

    }

    @PostConstruct
    public void initialize() {
        System.out.println("------------------------------------------------------");
        System.out.println("----------原神refresh-gpt-chat v0.2.0启动成功------------");
        System.out.println("1.新增打字机效果，优化流式输出");
        System.out.println("2.新增/v1/images/generations接口");
        System.out.println("3.新增/ninja/v1/images/generations接口");
        System.out.println("4.新增openai官网渠道使得refresh_token自动刷新成access_token");
        System.out.println("URL地址：http://0.0.0.0:" + serverPort + contextPath +"");
        System.out.println("------------------------------------------------------\n\n");
        System.out.println("---------------------参数设置--------------------------");
        System.out.println("参数enableOai:"+ enableOai);
        System.out.println("参数getAccessTokenUrl:"+getAccessTokenUrl);
        System.out.println("参数chatUrl:"+chatUrl);
        System.out.println("参数ninja_chatUrl:"+ninja_chatUrl);
        System.out.println("------------------------------------------------------");
    }
}

