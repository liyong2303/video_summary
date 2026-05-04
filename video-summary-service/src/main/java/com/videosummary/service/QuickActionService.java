package com.videosummary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.dto.QuickActionRequest;
import com.videosummary.dto.QuickActionResponse;
import com.videosummary.entity.QuickAction;
import com.videosummary.mapper.QuickActionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuickActionService {

    private final QuickActionMapper quickActionMapper;

    /**
     * 获取用户的所有快捷操作
     */
    public List<QuickActionResponse> getByUserId(Long userId) {
        List<QuickAction> actions = quickActionMapper.selectList(
                new LambdaQueryWrapper<QuickAction>()
                        .eq(QuickAction::getUserId, userId)
                        .orderByDesc(QuickAction::getCreatedAt)
        );

        return actions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 创建快捷操作
     */
    public QuickAction create(QuickActionRequest request, Long userId) {
        QuickAction action = QuickAction.builder()
                .userId(userId)
                .name(request.getName())
                .steps(request.getSteps())
                .applyScope(request.getApplyScope())
                .build();
        quickActionMapper.insert(action);
        return action;
    }

    /**
     * 更新快捷操作
     */
    public void update(Long id, QuickActionRequest request, Long userId) {
        QuickAction existing = quickActionMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("快捷操作不存在或无权修改");
        }

        QuickAction action = QuickAction.builder()
                .id(id)
                .userId(userId)
                .name(request.getName())
                .steps(request.getSteps())
                .applyScope(request.getApplyScope())
                .build();
        quickActionMapper.updateById(action);
    }

    /**
     * 删除快捷操作
     */
    public void delete(Long id, Long userId) {
        QuickAction action = quickActionMapper.selectById(id);
        if (action == null || !action.getUserId().equals(userId)) {
            throw new IllegalArgumentException("快捷操作不存在或无权删除");
        }
        quickActionMapper.deleteById(id);
    }

    /**
     * 获取快捷操作详情
     */
    public QuickActionResponse getById(Long id, Long userId) {
        QuickAction action = quickActionMapper.selectById(id);
        if (action == null || !action.getUserId().equals(userId)) {
            throw new IllegalArgumentException("快捷操作不存在或无权访问");
        }
        return toResponse(action);
    }

    private QuickActionResponse toResponse(QuickAction action) {
        return QuickActionResponse.builder()
                .id(action.getId())
                .name(action.getName())
                .steps(action.getSteps())
                .applyScope(action.getApplyScope())
                .createdAt(action.getCreatedAt())
                .updatedAt(action.getUpdatedAt())
                .build();
    }
}
