package com.videosummary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("task_result")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String outputType;

    private String content;

    private String modelUsed;

    private Integer inputTokens;

    private Integer outputTokens;

    private String status;

    public static class OutputType {
        public static final String SUMMARY = "summary";
        public static final String ARTICLE = "article";
        public static final String CARD = "card";
        public static final String XIAOHONGSHU = "xiaohongshu";
    }

    public static class Status {
        public static final String COMPLETED = "completed";
        public static final String FAILED = "failed";
        public static final String SKIPPED = "skipped";
    }
}
