package com.nju.edu.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class GitDTO {
    private String gitRepoUrl;

    private String branchName;
}
