package com.videosummary.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.videosummary.config.JsonTypeHandler;
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
@TableName(value = "quick_action", autoResultMap = true)
public class QuickAction {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    @TableField(typeHandler = JsonTypeHandler.class)
    private List<QuickActionStep> steps;

    private String applyScope;  // single/batch

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickActionStep {
        private String type;  // regenerate/export/copy
        private String outputType;  // for regenerate: all/summary/article...
        private String format;  // for export: pdf/word/markdown
        private String content;  // for copy: summary/article...
    }
}
