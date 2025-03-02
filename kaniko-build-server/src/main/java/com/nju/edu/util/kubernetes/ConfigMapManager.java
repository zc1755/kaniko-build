package com.nju.edu.util.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Component
public class ConfigMapManager {

    @Resource
    private KubernetesClient client;

    public String createConfigMap(String fileName, String content, String namespace) {
        String configMapName = "kaniko-configmap-" + UUID.randomUUID();
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(configMapName)
                .withNamespace(namespace)
                .endMetadata()
                .addToData(fileName, content)
                .build();
        client.configMaps().inNamespace(namespace).create(configMap);
        return configMapName;
    }

    public void deleteConfigMap(String configMapName, String namespace) {
        if(!isNotEmpty(configMapName)){
            return;
        }
        boolean isDeleted = client.configMaps().inNamespace(namespace).withName(configMapName).delete();

        if (isDeleted) {
            System.out.println("ConfigMap " + configMapName + " deleted successfully.");
        } else {
            System.out.println("Failed to delete ConfigMap " + configMapName + ".");
        }
    }
    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}
