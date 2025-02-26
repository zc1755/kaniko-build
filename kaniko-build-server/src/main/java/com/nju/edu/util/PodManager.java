package com.nju.edu.util;

import com.nju.edu.dao.KanikoBuildLogDAO;
import com.nju.edu.entities.KanikoDTO;
import com.nju.edu.entity.KanikoBuildLog;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class PodManager {
    @Resource
    KubernetesClient client;

    @Resource
    private KanikoUtil kanikoUtil;

    @Resource
    private ConfigMapManager configMapManager;

    @Resource
    private KanikoBuildLogDAO kanikoBuildLogDAO;

    @Resource
    private RocketMQProducer rocketMQProducer;

    private static final String TOPIC = "kaniko-build-topic";

    private final ConcurrentHashMap<String, Watch> watchMap = new ConcurrentHashMap<>();

    public String[] prepareConfigMaps(KanikoDTO kanikoDTO) {
        String[] configMapNames = new String[2];
        if (isNotEmpty(kanikoDTO.getConfigMapContent())) {
            configMapNames[0] = configMapManager.createConfigMap(kanikoDTO.getConfigMapContentName(), kanikoDTO.getConfigMapContent(), kanikoUtil.getNamespace());
        }
        if (isNotEmpty(kanikoDTO.getDockerfileContent())) {
            configMapNames[1] = configMapManager.createConfigMap("Dockerfile", kanikoDTO.getDockerfileContent(), kanikoUtil.getNamespace());
        }
        return configMapNames;
    }
    public void createAndMonitorPod(String podName,KanikoDTO kanikoDTO, String[] configMapNames) {
        Pod pod = createKanikoPod(podName, configMapNames, kanikoDTO);
        client.pods().inNamespace(kanikoUtil.getNamespace()).create(pod);

        // 启动监听
        watchPodStatus(kanikoDTO.getImageName(), podName, kanikoUtil.getNamespace(),configMapNames);
    }
    public String queryLog(String podName, String namespace){
        String logs = "";
        try {
            // 获取日志
            logs = client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .getLog();
            // 输出日志
            System.out.println("Pod Logs: \n" + logs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logs;
    }


    //因为podName基本不重复，所以synchronized暂时感觉没必要加
    public void watchPodStatus(String imageName, String podName, String namespace, String[] configMapNames) {
        // 在匿名内部类中创建和使用 watch 实例
        if (!watchMap.containsKey(podName)) {
            Watch watch = client.pods().inNamespace(namespace).withName(podName).watch(new Watcher<Pod>() {
                @Override
                public void eventReceived(Action action, Pod pod) {

                    PodStatus status = pod.getStatus();
                    String phase = status.getPhase();
                    // 根据 Pod 状态进行处理
                    switch (phase) {
                        case "Succeeded":
                            System.out.println("Pod " + podName + " finished successfully.");
                            String successLog = "Image built successfully.";
                            updateBuildStatus(podName, KanikoBuildLog.BuildStatus.SUCCESS, successLog);

                            notifySuccess(imageName, podName, namespace, configMapNames);
                            break;
                        case "Failed":
                            System.out.println("Pod " + podName + "   failed.");
                            String failureLog = queryLog(podName, namespace); // 获取详细失败日志
                            updateBuildStatus(podName, KanikoBuildLog.BuildStatus.FAILED, failureLog);

                            notifyFailure(imageName, podName, namespace, configMapNames);
                            break;
                        case "Running":
                            System.out.println("Pod " + podName + " is running.");
                            updateBuildStatus(podName, KanikoBuildLog.BuildStatus.IN_PROGRESS, null);
                            break;
                    }
                }

                //为了处理那些未显式关闭而出现错误或连接中断的情况
                @Override
                public void onClose(KubernetesClientException cause) {
                    System.out.println("Watcher closed: " + cause.getMessage());
                }
            });
            watchMap.put(podName, watch);
        }

    }
    private void updateBuildStatus(String podName, KanikoBuildLog.BuildStatus status, String logContent) {
        Optional<KanikoBuildLog> logOpt = kanikoBuildLogDAO.findByPodName(podName);
        if (logOpt.isPresent()) {
            KanikoBuildLog log = logOpt.get();
            log.setBuildStatus(status);
            log.setLog(logContent);
            kanikoBuildLogDAO.save(log);
        }
    }

    private void notifySuccess(String imageName, String podName, String namespace,String[] configMapNames) {
        // TODO:发送成功通知到调用方
//        rocketMQProducer.sendSuccessMessage(TOPIC,imageName);
        System.out.println("Image build successful.");
        clearResources(configMapNames,podName,namespace);
    }

    private void notifyFailure(String imageName, String podName, String namespace,String[] configMapNames) {
        // 发送失败通知到调用方
//        String reason = queryLog(podName, namespace);
//        rocketMQProducer.sendFailureMessage(TOPIC,imageName,reason);
        System.out.println("Image build failed.");
        clearResources(configMapNames,podName,namespace);
    }

    private void clearResources(String[] configMapNames,String podName, String namespace ){
        //清除相关资源
        configMapManager.deleteConfigMap(configMapNames[0],namespace);
        configMapManager.deleteConfigMap(configMapNames[1],namespace);
        client.pods().inNamespace(namespace).withName(podName).delete();
        Watch watch = watchMap.remove(podName);
        watch.close();
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
    public String generatePodName() {
        return "kaniko-build-pod-" + UUID.randomUUID();
    }
    private Pod createKanikoPod(String podName, String[] configMapNames, KanikoDTO kanikoDTO) {
        return new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .withNamespace(kanikoUtil.getNamespace())
                .endMetadata()
                .withNewSpec()
                .withContainers(createKanikoContainer(kanikoDTO))
                .withRestartPolicy("Never")
                .withVolumes(createVolumes(configMapNames,kanikoDTO))
                .endSpec()
                .build();
    }
    private Container createKanikoContainer(KanikoDTO kanikoDTO) {
        return new ContainerBuilder()
                .withName("kaniko")
                .withImage(kanikoUtil.getKanikoImage()) // 指定 Kaniko 镜像
                .withImagePullPolicy("IfNotPresent")
                .withArgs(
                        String.format("--context=git://$(GIT_USERNAME):$(GIT_PASSWORD)@%s#%s", kanikoDTO.getGitRepoUrl(), kanikoDTO.getBranchName()),
                        "--dockerfile=Dockerfile",
                        "--destination="+kanikoUtil.getDockerRegistry()+kanikoDTO.getImageName(),
                        "--cache=true",
                        "--cache-repo="+kanikoUtil.getCacheRepo(),
                        "--registry-mirror="+kanikoUtil.getRegistryMirror()
                )
                .withEnv(createEnvVars())
                .withVolumeMounts(createVolumeMounts(kanikoDTO))
                .build();
    }

    private List<EnvVar> createEnvVars() {
        return Arrays.asList(
                new EnvVarBuilder()
                        .withName("GIT_USERNAME")
                        .withNewValueFrom()
                        .withNewSecretKeyRef("username", kanikoUtil.getGitlabCredentials(), false)
                        .endValueFrom()
                        .build(),
                new EnvVarBuilder()
                        .withName("GIT_PASSWORD")
                        .withNewValueFrom()
                        .withNewSecretKeyRef("password", kanikoUtil.getGitlabCredentials(), false)
                        .endValueFrom()
                        .build(),
                new EnvVarBuilder()
                        .withName("DOCKER_CONFIG")
                        .withValue("/kaniko/.docker")
                        .build()
        );
    }

    private List<VolumeMount> createVolumeMounts(KanikoDTO kanikoDTO) {
        List<VolumeMount> volumeMounts = new ArrayList<>();

        // Kaniko Secret Volume Mount
        volumeMounts.add(new VolumeMountBuilder()
                .withName("kaniko-secret")
                .withMountPath("/kaniko/.docker")
                .build());

        // Dockerfile Override Volume Mount
        if (isNotEmpty(kanikoDTO.getDockerfileContent())) {
            volumeMounts.add(new VolumeMountBuilder()
                    .withName("dockerfile-override")
                    .withMountPath("/workspace/Dockerfile")
                    .withSubPath("Dockerfile")
                    .build());
        }

        // YAML Override Volume Mount
        if (isNotEmpty(kanikoDTO.getConfigMapContent())) {
            volumeMounts.add(new VolumeMountBuilder()
                    .withName("yaml-override")
                    .withMountPath("/workspace/config.yml")
                    .withSubPath("config.yml")
                    .build());
        }

        // Maven Settings Volume Mount
        volumeMounts.add(new VolumeMountBuilder()
                .withName("maven-settings")
                .withMountPath("/workspace/settings.xml")
                .withSubPath("settings.xml")
                .build());

//        // Maven Cache Volume Mount
//        volumeMounts.add(new VolumeMountBuilder()
//                .withName("maven-cache")
//                .withMountPath("/root/.m2")
//                .build());

        return volumeMounts;
    }


    private List<Volume> createVolumes(String[] configMapNames, KanikoDTO kanikoDTO) {
        List<Volume> volumes = new ArrayList<>();

        // Kaniko Secret Volume
        volumes.add(new VolumeBuilder()
                .withName("kaniko-secret")
                .withProjected(
                        new ProjectedVolumeSourceBuilder()
                                .withSources(
                                        new VolumeProjectionBuilder()
                                                .withSecret(new SecretProjectionBuilder()
                                                        .withName(kanikoUtil.getSecretName())
                                                        .withItems(new KeyToPathBuilder()
                                                                .withKey(".dockerconfigjson")
                                                                .withPath("config.json")
                                                                .build())
                                                        .build())
                                                .build())
                                .build())
                .build());
        // YAML Override Volume
        if (isNotEmpty(kanikoDTO.getConfigMapContent())) {
            volumes.add(new VolumeBuilder()
                    .withName("yaml-override")
                    .withConfigMap(new ConfigMapVolumeSourceBuilder()
                            .withName(configMapNames[0]) // Use provided name
                            .build())
                    .build());
        }

        // Dockerfile Override Volume
        if (isNotEmpty(kanikoDTO.getDockerfileContent())) {
            volumes.add(new VolumeBuilder()
                    .withName("dockerfile-override")
                    .withConfigMap(new ConfigMapVolumeSourceBuilder()
                            .withName(configMapNames[1]) // Replace with dynamic name if necessary
                            .build())
                    .build());
        }

        // Maven Settings Volume
        volumes.add(new VolumeBuilder()
                .withName("maven-settings")
                .withConfigMap(new ConfigMapVolumeSourceBuilder()
                        .withName("maven-settings")
                        .build())
                .build());

        // Maven Cache Volume
//        volumes.add(new VolumeBuilder()
//                .withName("maven-cache")
//                .withHostPath(new HostPathVolumeSourceBuilder()
//                        .withPath("/var/maven-cache/repository")
//                        .withType("DirectoryOrCreate")
//                        .build())
//                .build());

        return volumes;
    }
}
