package com.nju.edu.util;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RocketMQProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    public void sendSuccessMessage(String topic, String imageName) {
        String message = "Image build success: imageName=" + imageName;
        rocketMQTemplate.convertAndSend(topic, message);
        System.out.println("Success message sent: " + message);
    }

    public void sendFailureMessage(String topic, String imageName, String reason) {
        String message = String.format("Image build failed: [imageName=%s, reason=%s]", imageName, reason);
        rocketMQTemplate.convertAndSend(topic, message);
        System.out.println("Failure message sent: " + message);
    }
}

