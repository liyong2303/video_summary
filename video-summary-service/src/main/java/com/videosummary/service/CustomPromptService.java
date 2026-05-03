package com.videosummary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.entity.CustomPrompt;
import com.videosummary.mapper.CustomPromptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomPromptService {

    private final CustomPromptMapper customPromptMapper;

    /**
     * 获取用户的自定义 Prompt 列表
     */
    public List<CustomPrompt> getByUserId(Long userId) {
        return customPromptMapper.selectList(
                new LambdaQueryWrapper<CustomPrompt>()
                        .eq(CustomPrompt::getUserId, userId)
                        .orderByDesc(CustomPrompt::getCreatedAt)
        );
    }

    /**
     * 根据ID获取 Prompt
     */
    public CustomPrompt getById(Long id) {
        return customPromptMapper.selectById(id);
    }

    /**
     * 获取用户指定类型的默认自定义 Prompt
     */
    public CustomPrompt getDefaultByUserAndType(Long userId, String outputType) {
        return customPromptMapper.selectOne(
                new LambdaQueryWrapper<CustomPrompt>()
                        .eq(CustomPrompt::getUserId, userId)
                        .eq(CustomPrompt::getOutputType, outputType)
                        .eq(CustomPrompt::getIsDefault, true)
        );
    }

    /**
     * 保存自定义 Prompt
     */
    public void save(CustomPrompt prompt) {
        customPromptMapper.insert(prompt);
    }

    /**
     * 更新自定义 Prompt
     */
    public void update(CustomPrompt prompt) {
        customPromptMapper.updateById(prompt);
    }

    /**
     * 删除自定义 Prompt
     */
    public void delete(Long id, Long userId) {
        CustomPrompt prompt = customPromptMapper.selectById(id);
        if (prompt == null || !prompt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Prompt不存在或无权删除");
        }
        customPromptMapper.deleteById(id);
    }

    /**
     * 设置默认 Prompt
     */
    public void setDefault(Long id, Long userId, String outputType) {
        // 清除该用户该类型的所有默认标记
        customPromptMapper.update(null,
                new LambdaQueryWrapper<CustomPrompt>()
                        .eq(CustomPrompt::getUserId, userId)
                        .eq(CustomPrompt::getOutputType, outputType)
                        .eq(CustomPrompt::getIsDefault, true),
                new com.baomidou.mybatisplus.core.update.LambdaUpdateWrapper<CustomPrompt>()
                        .set(CustomPrompt::getIsDefault, false)
        );
        // 设置新的默认
        CustomPrompt prompt = customPromptMapper.selectById(id);
        if (prompt != null && prompt.getUserId().equals(userId)) {
            prompt.setIsDefault(true);
            customPromptMapper.updateById(prompt);
        }
    }
}
