package com.videosummary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSubmitResponse {
    private String batchId;
    private List<BatchTaskItem> tasks;
    private String status;  // processing/completed/failed

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchTaskItem {
        private Long taskId;
        private String url;
        private String status;
        private String error;
    }
}
