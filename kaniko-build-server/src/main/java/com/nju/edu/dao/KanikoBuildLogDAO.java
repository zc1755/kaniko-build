package com.nju.edu.dao;


import com.nju.edu.entity.KanikoBuildLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KanikoBuildLogDAO extends JpaRepository<KanikoBuildLog, Long> {

    /**
     * 根据镜像名称查询构建记录
     *
     * @param imageName 镜像名称
     * @return 构建记录列表
     */
    Optional<KanikoBuildLog> findByImageName(String imageName);

    /**
     * 根据构建状态查询构建记录
     *
     * @param buildStatus 构建状态
     * @return 构建记录列表
     */
    KanikoBuildLog findByBuildStatus(KanikoBuildLog.BuildStatus buildStatus);

    Optional<KanikoBuildLog> findByPodName(String podName);
}