package com.videosummary.bilibili.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoInfo {
    private String bvid;
    private String title;
    private Integer duration;
    private String coverUrl;
    private Long cid;
    private String owner;
}
