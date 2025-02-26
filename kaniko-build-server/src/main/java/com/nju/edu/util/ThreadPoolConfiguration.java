package com.nju.edu.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfiguration {
    @Resource
    KanikoUtil kanikoUtil;

    @Bean
    public ExecutorService executorService() {
        // 使用配置文件中的大小来创建固定大小的线程池
        return Executors.newFixedThreadPool(kanikoUtil.getThreadPoolSize());
    }
}
