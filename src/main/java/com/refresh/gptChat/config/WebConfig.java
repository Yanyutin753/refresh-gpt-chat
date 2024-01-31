package com.refresh.gptChat.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置默认异步请求超时时间，例如设置为6分钟
        configurer.setDefaultTimeout(TimeUnit.MINUTES.toMillis(6));
    }
}
