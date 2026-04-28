package com.videosummary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String phone;

    private String passwordHash;

    private String role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static class Role {
        public static final String FREE = "free";
        public static final String PAID = "paid";
        public static final String ADMIN = "admin";
    }
}
