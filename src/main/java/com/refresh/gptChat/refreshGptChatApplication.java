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

    public int getMaxThreads() {
        return max_threads;
    }


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

    public static void main(String[] args) {
        SpringApplication.run(refreshGptChatApplication.class, args);

    }

    @PostConstruct
    public void initialize() {
        System.out.println("------------------------------------------------------");
        System.out.println("---------------------参数设置--------------------------");
        System.out.println("参数serverPort："+ serverPort);
        System.out.println("参数prefix："+ prefix);
        System.out.println("参数max_threads："+ max_threads);
        System.out.println("参数isCancelGizmo："+ isCancelGizmo);
        System.out.println("参数getAccessTokenService："+ getAccessTokenService);
        System.out.println("参数getAccessTokenUrl_ninja："+ getAccessTokenUrl_ninja);
        System.out.println("");
        System.out.println("----------原神refresh-gpt-chat v0.3.0启动成功------------");
        System.out.println("1.新增打字机效果，优化流式输出");
        System.out.println("2.新增/v1/images/generations接口");
        System.out.println("3.新增refresh_token,requrest_url,ChatGPT-Account-ID 形式传入接口，用于自定义refresh_token,请求地址和ChatGPT-Account-ID\n" +
                "   例如 Bearer asdasdasdasdwe2132ewe,https://api.oaifree.com/v1/chat/completions,asdasdasdasdasd12313");
        System.out.println("4.新增openai和xyhelper渠道使得refresh_token自动刷新成access_token");
        System.out.println("URL地址：http://0.0.0.0:" + serverPort + prefix + "");
        System.out.println("------------------------------------------------------");
    }
}

