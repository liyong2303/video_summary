package com.videosummary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.entity.TemplateCategory;
import com.videosummary.mapper.TemplateCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateCategoryService {

    private final TemplateCategoryMapper templateCategoryMapper;

    /**
     * 获取用户的所有分类
     */
    public List<TemplateCategory> getByUserId(Long userId) {
        return templateCategoryMapper.selectList(
                new LambdaQueryWrapper<TemplateCategory>()
                        .eq(TemplateCategory::getUserId, userId)
                        .orderByAsc(TemplateCategory::getSortOrder)
        );
    }

    /**
     * 创建分类
     */
    public TemplateCategory create(TemplateCategory category) {
        templateCategoryMapper.insert(category);
        return category;
    }

    /**
     * 更新分类
     */
    public void update(TemplateCategory category) {
        templateCategoryMapper.updateById(category);
    }

    /**
     * 删除分类
     */
    public void delete(Long id, Long userId) {
        TemplateCategory category = templateCategoryMapper.selectById(id);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new IllegalArgumentException("分类不存在或无权删除");
        }
        templateCategoryMapper.deleteById(id);
    }
}
