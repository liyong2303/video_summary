package com.videosummary.dto;

import com.videosummary.entity.QuickAction;
import lombok.Data;

import java.util.List;

@Data
public class QuickActionRequest {
    private String name;
    private List<QuickAction.QuickActionStep> steps;
    private String applyScope;  // single/batch
}
