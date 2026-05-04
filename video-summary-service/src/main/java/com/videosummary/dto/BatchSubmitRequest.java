package com.videosummary.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchSubmitRequest {
    @NotEmpty(message = "视频链接不能为空")
    @Size(max = 5, message = "最多支持5个视频")
    private List<String> urls;

    private String style = "concise";

    private String length = "standard";
}
