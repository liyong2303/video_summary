package com.videosummary.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@TableName("user_preference")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String style;  // academic/casual/concise

    private String length;  // short/standard/long

    @TableField(typeHandler = com.videosummary.config.JsonTypeHandler.class)
    private List<String> outputTypes;  // JSON 数组

    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private java.time.LocalDateTime updatedAt;
}
