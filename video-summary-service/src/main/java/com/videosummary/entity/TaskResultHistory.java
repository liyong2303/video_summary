package com.videosummary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("task_result_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskResultId;

    private Long taskId;

    private String outputType;

    private String content;

    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;
}
