package com.videosummary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("custom_prompt")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomPrompt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String outputType;  // summary/article/card/xiaohongshu

    private String systemPrompt;

    private String userPrompt;

    private Boolean isDefault;

    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private java.time.LocalDateTime updatedAt;
}
