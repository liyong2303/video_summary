package com.videosummary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.entity.UserPreference;
import com.videosummary.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceMapper userPreferenceMapper;

    /**
     * 获取用户偏好，如果不存在则返回默认值
     */
    public UserPreference getOrDefault(Long userId) {
        UserPreference pref = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );
        if (pref == null) {
            pref = UserPreference.builder()
                    .userId(userId)
                    .style("concise")
                    .length("standard")
                    .outputTypes(Arrays.asList("summary", "article", "card", "xiaohongshu"))
                    .build();
        }
        return pref;
    }

    /**
     * 保存或更新用户偏好
     */
    public void saveOrUpdate(UserPreference preference) {
        UserPreference existing = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, preference.getUserId())
        );
        if (existing != null) {
            preference.setId(existing.getId());
            userPreferenceMapper.updateById(preference);
        } else {
            userPreferenceMapper.insert(preference);
        }
    }
}
