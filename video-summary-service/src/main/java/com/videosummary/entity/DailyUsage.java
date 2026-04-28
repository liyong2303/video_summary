package com.videosummary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@TableName("daily_usage")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate usageDate;

    private Integer count;
}
