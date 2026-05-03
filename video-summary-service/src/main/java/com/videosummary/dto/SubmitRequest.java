package com.videosummary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitRequest {
    @NotBlank(message = "视频链接不能为空")
    private String url;

    private String style = "concise";  // academic/casual/concise

    private String length = "standard";  // short/standard/long
}
