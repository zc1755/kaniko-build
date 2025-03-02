package com.nju.edu.util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class SemaphoreRateLimiter {
    private final Semaphore semaphore;
    private final long timeoutSeconds;

    @Autowired
    public SemaphoreRateLimiter(ApplicationProperties applicationProperties) {
        this.semaphore = new Semaphore(applicationProperties.getMaxConcurrent());
        this.timeoutSeconds = applicationProperties.getTimeoutSeconds();
    }

    /**
     * 尝试获取信号量，如果超时，则返回 false
     */
    public boolean tryAcquire() {
        try {
            return semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放信号量，允许新的任务进入
     */
    public void release() {
        semaphore.release();
    }
}
