package com.videosummary.bilibili.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleContent {
    private Double from;
    private Double to;
    private String content;
}
