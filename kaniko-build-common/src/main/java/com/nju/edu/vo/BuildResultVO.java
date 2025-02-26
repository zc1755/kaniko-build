package com.nju.edu.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BuildResultVO {
    private String imageName;

    private String buildStatus;

    private String log;
}
