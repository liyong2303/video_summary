package com.videosummary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("task")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String bvid;

    private Long cid;

    private String videoTitle;

    private Integer videoDuration;

    private String coverUrl;

    private String subtitleStoragePath;

    private String status;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public static class Status {
        public static final String PENDING = "pending";
        public static final String PROCESSING = "processing";
        public static final String COMPLETED = "completed";
        public static final String PARTIALLY_COMPLETED = "partially_completed";
        public static final String FAILED = "failed";
        public static final String CANCELLED = "cancelled";
    }
}
