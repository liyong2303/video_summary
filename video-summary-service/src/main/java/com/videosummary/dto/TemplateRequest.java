package com.videosummary.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateRequest {
    private Long categoryId;
    private String name;
    private String style;
    private String length;
    private List<String> outputTypes;
    private List<Long> customPromptIds;
}
