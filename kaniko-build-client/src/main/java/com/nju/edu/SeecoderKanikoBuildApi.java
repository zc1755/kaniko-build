package com.nju.edu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nju.edu.vo.BuildResultVO;

public interface SeecoderKanikoBuildApi {
    void startBuild(String gitRepoUrl, String branchName, String imageName, String configMapContentName, String configMapContent, String dockerfileContent);
    BuildResultVO queryBuild(String imageName);
}
