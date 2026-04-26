package com.videosummary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResponse {
    private Long taskId;
    private String bvid;
    private String videoTitle;
    private Integer videoDuration;
    private String coverUrl;
    private String status;
    private Boolean isExisting;
}
