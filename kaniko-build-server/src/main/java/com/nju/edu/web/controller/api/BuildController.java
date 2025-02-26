package com.nju.edu.web.controller.api;

import com.nju.edu.entities.GitDTO;
import com.nju.edu.entities.KanikoDTO;
import com.nju.edu.model.Response;
import com.nju.edu.resp.ResultData;
import com.nju.edu.service.BuildService;
import com.nju.edu.vo.BuildResultVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/builds")

public class BuildController {

    @Resource
    BuildService buildService;

//    @PostMapping("")
//    public ResultData<String> startBuild(@RequestBody GitDTO gitDTO){
//        buildService.startBuild(gitDTO.getGitRepoUrl(),gitDTO.getBranchName());
//        return ResultData.success("完成构建");
//    }

    @PostMapping("")
    public Response startBuild(@RequestBody KanikoDTO kanikoDTO){
        buildService.startBuild(kanikoDTO);
        return Response.buildSuccess("完成构建");
    }

    @GetMapping("/query")
    public Response queryBuildByImageName(@RequestParam("imageName") String imageName){
        return Response.buildSuccess(buildService.queryBuildByImageName(imageName));
    }

}
