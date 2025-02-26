package com.nju.edu.service;

import com.nju.edu.entities.KanikoDTO;
import com.nju.edu.vo.BuildResultVO;

public interface BuildService {

    void  startBuild(KanikoDTO kanikoDTO);


    BuildResultVO queryBuildByImageName(String imageName);
}
