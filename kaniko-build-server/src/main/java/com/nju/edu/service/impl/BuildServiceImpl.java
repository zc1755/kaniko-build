package com.nju.edu.service.impl;

import com.nju.edu.dao.KanikoBuildLogDAO;
import com.nju.edu.entities.KanikoDTO;
import com.nju.edu.entity.KanikoBuildLog;
import com.nju.edu.service.BuildService;
import com.nju.edu.util.KanikoUtil;
import com.nju.edu.util.PodManager;
import com.nju.edu.vo.BuildResultVO;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;

@Service
public class BuildServiceImpl implements BuildService {

    @Resource
    private KanikoUtil kanikoUtil;

    @Resource
    private KubernetesClient client;

    @Resource
    private ExecutorService executorService;

    @Resource
    private PodManager podManager;

    @Resource
    private KanikoBuildLogDAO kanikoBuildLogDAO;

    @Override
    public void startBuild(KanikoDTO kanikoDTO) {
        String podName = podManager.generatePodName();
        String[] configMapNames = podManager.prepareConfigMaps(kanikoDTO);
        // 创建 Pod 并启动监控
        podManager.createAndMonitorPod(podName, kanikoDTO, configMapNames);
        // 检查是否存在相同的镜像名
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
                String logStr = podManager.queryLog(podName,kanikoUtil.getNamespace());
                resultVO.setLog(logStr);
            }
        }
        return resultVO;
    }

}
