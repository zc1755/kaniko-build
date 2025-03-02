package com.nju.edu.service.impl;

import com.nju.edu.dao.KanikoBuildLogDAO;
import com.nju.edu.entities.KanikoDTO;
import com.nju.edu.entity.KanikoBuildLog;
import com.nju.edu.service.BuildService;
import com.nju.edu.util.ApplicationProperties;
import com.nju.edu.util.SemaphoreRateLimiter;
import com.nju.edu.util.ServiceException;
import com.nju.edu.util.kubernetes.ConfigMapManager;
import com.nju.edu.util.kubernetes.PodManager;
import com.nju.edu.vo.BuildResultVO;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BuildServiceImpl implements BuildService {

    @Resource
    private ApplicationProperties applicationProperties;

    @Resource
    private KubernetesClient client;

    @Resource
    private PodManager podManager;

    @Resource
    private KanikoBuildLogDAO kanikoBuildLogDAO;
    @Resource
    private SemaphoreRateLimiter semaphoreRateLimiter;
    @Resource
    private ConfigMapManager configMapManager;
    @Override
    public void startBuild(KanikoDTO kanikoDTO) {
        String podName = podManager.generatePodName();
        boolean podStarted = false;
        if (kanikoDTO == null) {
            throw  new ServiceException("400", "请求参数不能为空");
        }
        if (!semaphoreRateLimiter.tryAcquire()) {
            System.out.println("资源不足，构建请求被限流：" + kanikoDTO.getImageName());
            throw new ServiceException("429", "当前并发构建任务已达上限，请稍后重试");
        }
        try{
            String[] configMapNames = prepareConfigMaps(kanikoDTO);
            podManager.createAndMonitorPod(podName, kanikoDTO, configMapNames);
            podStarted = true;

            Optional<KanikoBuildLog> existingLog = kanikoBuildLogDAO.findByImageName(kanikoDTO.getImageName());
            KanikoBuildLog log;
            if (existingLog.isPresent()) {
                // 更新已有记录
                log = existingLog.get();
                log.setPodName(podName);
                log.setBuildStatus(KanikoBuildLog.BuildStatus.PENDING);
                log.setLog(null); // 清空旧的日志
                log.setUpdatedAt(LocalDateTime.now());
            } else {
                // 创建新记录
                log = new KanikoBuildLog();
                log.setImageName(kanikoDTO.getImageName());
                log.setPodName(podName);
                log.setBuildStatus(KanikoBuildLog.BuildStatus.PENDING);
            }
            // 保存记录
            kanikoBuildLogDAO.save(log);
        } catch (DataAccessException e) {
            if (!podStarted) {
                semaphoreRateLimiter.release();
            }
            throw new ServiceException("500", "数据库异常：" + e.getMessage());

        } catch (Exception e) {
            if (!podStarted) {
                semaphoreRateLimiter.release();
            }
            throw new ServiceException("500", "服务器内部错误：" + e.getMessage());
        }


    }

    @Override
    public BuildResultVO queryBuildByImageName(String imageName){
        Optional<KanikoBuildLog> existingLog = kanikoBuildLogDAO.findByImageName(imageName);
        BuildResultVO resultVO = new BuildResultVO();
        resultVO.setImageName(imageName);
        if(existingLog.isPresent()){
            KanikoBuildLog log = existingLog.get();

            if(log.getBuildStatus().equals(KanikoBuildLog.BuildStatus.FAILED)||log.getBuildStatus().equals(KanikoBuildLog.BuildStatus.SUCCESS)){
                resultVO.setBuildStatus(String.valueOf(log.getBuildStatus()));
                resultVO.setLog(log.getLog());
            }else {
                resultVO.setBuildStatus(String.valueOf(log.getBuildStatus()));
                String podName = log.getPodName();
                String logStr = podManager.queryLog(podName, applicationProperties.getNamespace());
                resultVO.setLog(logStr);
            }
        }
        return resultVO;
    }
    private String[] prepareConfigMaps(KanikoDTO kanikoDTO) {
        String[] configMapNames = new String[2];
        if (isNotEmpty(kanikoDTO.getConfigMapContent())) {
            try {
                configMapNames[0] = configMapManager.createConfigMap(
                        kanikoDTO.getConfigMapContentName(),
                        kanikoDTO.getConfigMapContent(),
                        applicationProperties.getNamespace());
            } catch (Exception e) {
                throw new ServiceException("500", "创建 ConfigMap 失败：" + e.getMessage());
            }
        }
        if (isNotEmpty(kanikoDTO.getDockerfileContent())) {
            try {
                configMapNames[1] = configMapManager.createConfigMap(
                        "Dockerfile",
                        kanikoDTO.getDockerfileContent(),
                        applicationProperties.getNamespace());
            } catch (Exception e) {
                throw new ServiceException("500", "创建 Dockerfile ConfigMap 失败：" + e.getMessage());
            }
        }
        return configMapNames;
    }
    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}
