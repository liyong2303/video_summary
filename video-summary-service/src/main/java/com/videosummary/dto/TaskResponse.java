package com.videosummary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long taskId;
    private String bvid;
    private String videoTitle;
    private Integer videoDuration;
    private String coverUrl;
    private String status;
    private String subtitleText;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
