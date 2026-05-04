package com.videosummary.dto;

import com.videosummary.entity.QuickAction;
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
public class QuickActionResponse {
    private Long id;
    private String name;
    private List<QuickAction.QuickActionStep> steps;
    private String applyScope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
