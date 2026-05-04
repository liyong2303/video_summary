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
public class TemplateResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String style;
    private String length;
    private List<String> outputTypes;
    private List<Long> customPromptIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
