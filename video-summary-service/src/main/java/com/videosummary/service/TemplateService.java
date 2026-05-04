package com.videosummary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.dto.TemplateRequest;
import com.videosummary.dto.TemplateResponse;
import com.videosummary.entity.Template;
import com.videosummary.entity.TemplateCategory;
import com.videosummary.mapper.TemplateMapper;
import com.videosummary.mapper.TemplateCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;
    private final TemplateCategoryMapper templateCategoryMapper;

    /**
     * 获取用户的所有模板
     */
    public List<TemplateResponse> getByUserId(Long userId) {
        List<Template> templates = templateMapper.selectList(
                new LambdaQueryWrapper<Template>()
                        .eq(Template::getUserId, userId)
                        .orderByDesc(Template::getCreatedAt)
        );

        return templates.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 创建模板
     */
    public Template create(TemplateRequest request, Long userId) {
        Template template = Template.builder()
                .userId(userId)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .style(request.getStyle())
                .length(request.getLength())
                .outputTypes(request.getOutputTypes())
                .customPromptIds(request.getCustomPromptIds())
                .build();
        templateMapper.insert(template);
        return template;
    }

    /**
     * 更新模板
     */
    public void update(Long id, TemplateRequest request, Long userId) {
        Template existing = templateMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("模板不存在或无权修改");
        }

        Template template = Template.builder()
                .id(id)
                .userId(userId)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .style(request.getStyle())
                .length(request.getLength())
                .outputTypes(request.getOutputTypes())
                .customPromptIds(request.getCustomPromptIds())
                .build();
        templateMapper.updateById(template);
    }

    /**
     * 删除模板
     */
    public void delete(Long id, Long userId) {
        Template template = templateMapper.selectById(id);
        if (template == null || !template.getUserId().equals(userId)) {
            throw new IllegalArgumentException("模板不存在或无权删除");
        }
        templateMapper.deleteById(id);
    }

    /**
     * 获取模板详情
     */
    public TemplateResponse getById(Long id, Long userId) {
        Template template = templateMapper.selectById(id);
        if (template == null || !template.getUserId().equals(userId)) {
            throw new IllegalArgumentException("模板不存在或无权访问");
        }
        return toResponse(template);
    }

    private TemplateResponse toResponse(Template template) {
        TemplateResponse.TemplateResponseBuilder builder = TemplateResponse.builder()
                .id(template.getId())
                .categoryId(template.getCategoryId())
                .name(template.getName())
                .style(template.getStyle())
                .length(template.getLength())
                .outputTypes(template.getOutputTypes())
                .customPromptIds(template.getCustomPromptIds())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt());

        if (template.getCategoryId() != null) {
            TemplateCategory category = templateCategoryMapper.selectById(template.getCategoryId());
            if (category != null) {
                builder.categoryName(category.getName());
            }
        }

        return builder.build();
    }
}
