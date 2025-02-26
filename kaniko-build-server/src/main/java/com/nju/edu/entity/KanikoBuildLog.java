package com.nju.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@Entity
@Table(name = "kaniko_build_logs")
public class KanikoBuildLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "image_name", nullable = false, length = 255)
    private String imageName;

    @Column(name = "pod_name", nullable = false, length = 255)
    private String podName;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_status", nullable = false, length = 20)
    private BuildStatus buildStatus;

    @Lob
    @Column(name = "log")
    private String log;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BuildStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED
    }

}
