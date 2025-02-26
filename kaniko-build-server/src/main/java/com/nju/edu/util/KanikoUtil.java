package com.nju.edu.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "seecoder-build-server.kaniko")
public class KanikoUtil {
    private String namespace;
    private String kanikoImage;
    private String dockerRegistry;
    private String cacheRepo;
    private String registryMirror;
    private String secretName;
    private String mavenCachePath;
    private String gitlabCredentials;
    private int threadPoolSize;
    private K8s k8s = new K8s();            // Kubernetes 配置信息
    @Data
    public static class K8s {

        private String apiServer;           // Kubernetes API Server 地址
        private String token;               // Kubernetes 访问 token
        private boolean validateSSL = false; // 是否验证 SSL
        private boolean debug = true;       // 是否启用调试模式
//        private String imageRegistry;       // 镜像注册中心地址
//        private String secretName;          // 用于认证的 Kubernetes Secret 名称
    }

}
