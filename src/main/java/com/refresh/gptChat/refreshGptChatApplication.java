package com.refresh.gptChat;


import lombok.Data;
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
@Data
@EnableScheduling
@SpringBootApplication
public class refreshGptChatApplication {
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

    /**
     * 获取个人的uuid
     */
    @Value("${self_server_uuid}")
    private String self_server_uuid;

    /**
     * 画图使用的模型 gpt-4-mobile 或者 gpt-4
     */
    @Value("${image_mobel}")
    private String image_mobel;

    public static void main(String[] args) {
        SpringApplication.run(refreshGptChatApplication.class, args);

    }

    @PostConstruct
    public void initialize() {
        System.out.println("------------------------------------------------------");
        System.out.println("---------------------参数设置--------------------------");
        System.out.println("参数serverPort：" + serverPort);
        System.out.println("参数prefix：" + prefix);
        System.out.println("参数image_mobel：" + image_mobel);
        System.out.println("参数max_threads：" + max_threads);
        System.out.println("参数isCancelGizmo：" + isCancelGizmo);
        System.out.println("参数getAccessTokenService：" + getAccessTokenService);
        if ("ninja".equalsIgnoreCase(getAccessTokenService)) {
            System.out.println("参数getAccessTokenUrl_ninja：" + getAccessTokenUrl_ninja);
        }
        System.out.println("参数self_server_uuid：" + self_server_uuid);
        System.out.println();
        System.out.println("----------原神refresh-gpt-chat v0.7.0启动成功------------");
        System.out.println("1.新增oaifree作为服务商，支持refresh_token自动刷新成access_token");
        System.out.println("2.新增接口**/getAccountID**，获取ChatGPT-Account-ID");
        System.out.println("3.新增画图dall-e-3接口/v1/images/generations\n" +
                "4.新增文字转语音接口/v1/audio/speech\"\n" +
                "5.新增语言转文字接口/v1/audio/transcriptions");
        System.out.println("6.重构代码，逻辑更加清晰，结构更加合理");
        System.out.println("7.新增变量/v1/images/edits，用于控制图片编辑调用模型");
        System.out.println("URL地址：http://0.0.0.0:" + serverPort + prefix);
        System.out.println("------------------------------------------------------");
    }
}

