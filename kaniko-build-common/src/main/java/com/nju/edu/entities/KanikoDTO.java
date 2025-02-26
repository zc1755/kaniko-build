package com.nju.edu.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class KanikoDTO {
    private String gitRepoUrl;

    private String branchName;

    private String imageName;

    private String configMapContentName;

    private String configMapContent;

    private String dockerfileContent;
}
