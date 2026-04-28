package com.videosummary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItem {
    private Long taskId;
    private String bvid;
    private String videoTitle;
    private Integer videoDuration;
    private String coverUrl;
    private String status;
    private List<String> outputTypes;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
