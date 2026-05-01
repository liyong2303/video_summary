package com.videosummary.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.entity.DailyUsage;
import com.videosummary.entity.User;
import com.videosummary.mapper.DailyUsageMapper;
import com.videosummary.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class QuotaService {

    private static final int FREE_DAILY_LIMIT = 3;

    private final DailyUsageMapper dailyUsageMapper;
    private final UserMapper userMapper;

    public QuotaService(DailyUsageMapper dailyUsageMapper, UserMapper userMapper) {
        this.dailyUsageMapper = dailyUsageMapper;
        this.userMapper = userMapper;
    }

    public Long getCurrentUserId() {
        if (!StpUtil.isLogin()) return null;
        return StpUtil.getLoginIdAsLong();
    }

    public void checkQuota(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("用户不存在");
        }
        if (!User.Role.FREE.equals(user.getRole())) return;

        DailyUsage usage = dailyUsageMapper.selectOne(
                new LambdaQueryWrapper<DailyUsage>()
                        .eq(DailyUsage::getUserId, userId)
                        .eq(DailyUsage::getUsageDate, LocalDate.now())
        );
        int count = usage == null ? 0 : usage.getCount();
        if (count >= FREE_DAILY_LIMIT) {
            throw new QuotaExceededException(
                    String.format("今日免费额度已用完（%d/%d），明日0点重置", count, FREE_DAILY_LIMIT)
            );
        }
    }

    @Transactional
    public void checkAndIncrement(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("用户不存在");
        }
        // admin/paid 用户跳过限制，直接记录用量
        if (!User.Role.FREE.equals(user.getRole())) {
            doIncrement(userId);
            return;
        }

        DailyUsage usage = dailyUsageMapper.selectOne(
                new LambdaQueryWrapper<DailyUsage>()
                        .eq(DailyUsage::getUserId, userId)
                        .eq(DailyUsage::getUsageDate, LocalDate.now())
        );
        int count = usage == null ? 0 : usage.getCount();
        if (count >= FREE_DAILY_LIMIT) {
            throw new QuotaExceededException(
                    String.format("今日免费额度已用完（%d/%d），明日0点重置", count, FREE_DAILY_LIMIT)
            );
        }
        doIncrement(userId);
    }

    private void doIncrement(Long userId) {
        DailyUsage existing = dailyUsageMapper.selectOne(
                new LambdaQueryWrapper<DailyUsage>()
                        .eq(DailyUsage::getUserId, userId)
                        .eq(DailyUsage::getUsageDate, LocalDate.now())
        );
        if (existing == null) {
            dailyUsageMapper.insert(DailyUsage.builder()
                    .userId(userId)
                    .usageDate(LocalDate.now())
                    .count(1)
                    .build());
        } else {
            existing.setCount(existing.getCount() + 1);
            dailyUsageMapper.updateById(existing);
        }
    }

    @Transactional
    public void increment(Long userId) {
        doIncrement(userId);
    }

    public int getDailyLimit(String role) {
        return User.Role.FREE.equals(role) ? FREE_DAILY_LIMIT : -1;
    }

    @Scheduled(cron = "0 1 0 * * *")
    public void resetDailyUsage() {
        int deleted = dailyUsageMapper.delete(
                new LambdaQueryWrapper<DailyUsage>()
                        .lt(DailyUsage::getUsageDate, LocalDate.now())
        );
        log.info("Daily quota reset: deleted {} records", deleted);
    }

    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String message) {
            super(message);
        }
    }
}
