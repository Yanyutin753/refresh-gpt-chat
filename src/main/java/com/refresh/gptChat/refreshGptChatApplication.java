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


    public static void main(String[] args) {
        SpringApplication.run(refreshGptChatApplication.class, args);

    }

    @PostConstruct
    public void initialize() {
        log.info("------------------------------------------------------");
        log.info("----------原神refresh-gpt-chat v0.1.0启动成功------------");
        log.info("1.新增打字机效果，优化流式输出");
        log.info("2.新增/v1/images/generations接口");
        log.info("3.新增/ninja/v1/images/generations接口");
        log.info("URL地址：http://0.0.0.0:" + serverPort + contextPath +"");
        log.info("------------------------------------------------------");
    }
}

