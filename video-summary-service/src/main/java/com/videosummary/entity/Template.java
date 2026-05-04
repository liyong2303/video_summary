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
@TableName(value = "template", autoResultMap = true)
public class Template {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String name;

    private String style;  // academic/casual/concise

    private String length;  // short/standard/long

    @TableField(typeHandler = JsonTypeHandler.class)
    private List<String> outputTypes;

    @TableField(typeHandler = JsonTypeHandler.class)
    private List<Long> customPromptIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
