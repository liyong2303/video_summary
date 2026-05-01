package com.videosummary.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.dto.UserInfoResponse;
import com.videosummary.entity.DailyUsage;
import com.videosummary.entity.User;
import com.videosummary.mapper.DailyUsageMapper;
import com.videosummary.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class AuthService {

    private static final int FREE_DAILY_LIMIT = 3;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final DailyUsageMapper dailyUsageMapper;

    public AuthService(UserMapper userMapper, DailyUsageMapper dailyUsageMapper) {
        this.userMapper = userMapper;
        this.dailyUsageMapper = dailyUsageMapper;
    }

    public String register(String username, String rawPassword) {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(User.Role.FREE)
                .build();
        userMapper.insert(user);
        StpUtil.login(user.getId());
        log.info("User {} registered, id={}", username, user.getId());
        return StpUtil.getTokenValue();
    }

    public String login(String username, String rawPassword) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        StpUtil.login(user.getId());
        log.info("User {} logged in", username);
        return StpUtil.getTokenValue();
    }

    public UserInfoResponse getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("用户不存在");
        }
        int todayUsed = getTodayUsed(userId);
        int dailyLimit = User.Role.FREE.equals(user.getRole()) ? FREE_DAILY_LIMIT : -1;
        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .todayUsed(todayUsed)
                .dailyLimit(dailyLimit)
                .build();
    }

    private int getTodayUsed(Long userId) {
        DailyUsage usage = dailyUsageMapper.selectOne(
                new LambdaQueryWrapper<DailyUsage>()
                        .eq(DailyUsage::getUserId, userId)
                        .eq(DailyUsage::getUsageDate, LocalDate.now())
        );
        return usage == null ? 0 : usage.getCount();
    }
}
