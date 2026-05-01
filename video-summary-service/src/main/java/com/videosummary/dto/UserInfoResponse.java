package com.videosummary.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoResponse {
    private Long userId;
    private String username;
    private String role;
    private Integer todayUsed;
    private Integer dailyLimit;   // -1 = unlimited
}
