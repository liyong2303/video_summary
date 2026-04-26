package com.videosummary.bilibili.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleInfo {
    private String lang;
    private String langKey;
    private String url;
}
