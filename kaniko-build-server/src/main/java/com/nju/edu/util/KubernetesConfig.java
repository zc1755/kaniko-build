package com.nju.edu.util;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class KubernetesConfig {

    @Resource
    KanikoUtil kanikoUtil;

    @Bean
    public KubernetesClient kubernetesClient() {
        Config config = new ConfigBuilder()
                .withMasterUrl(kanikoUtil.getK8s().getApiServer())
                .withOauthToken(kanikoUtil.getK8s().getToken())
                .withTrustCerts(!kanikoUtil.getK8s().isValidateSSL())
                .build();

        return new DefaultKubernetesClient(config); // 创建一次 KubernetesClient 实例并交给 Spring 管理
    }

}
