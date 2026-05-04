package com.videosummary.controller;

import com.videosummary.dto.ApiResult;
import com.videosummary.dto.TemplateRequest;
import com.videosummary.dto.TemplateResponse;
import com.videosummary.entity.TemplateCategory;
import com.videosummary.service.QuotaService;
import com.videosummary.service.TemplateCategoryService;
import com.videosummary.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for template management.
 */
@Slf4j
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateCategoryService templateCategoryService;
    private final QuotaService quotaService;

    /**
     * Get current user's templates.
     */
    @GetMapping
    public ApiResult<List<TemplateResponse>> getTemplates() {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        return ApiResult.success(templateService.getByUserId(userId));
    }

    /**
     * Get template by ID.
     */
    @GetMapping("/{id}")
    public ApiResult<TemplateResponse> getTemplate(@PathVariable Long id) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        return ApiResult.success(templateService.getById(id, userId));
    }

    /**
     * Create a new template.
     */
    @PostMapping
    public ApiResult<TemplateResponse> createTemplate(@RequestBody TemplateRequest request) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        Template template = templateService.create(request, userId);
        return ApiResult.success(templateService.getById(template.getId(), userId));
    }

    /**
     * Update a template.
     */
    @PutMapping("/{id}")
    public ApiResult<TemplateResponse> updateTemplate(
            @PathVariable Long id,
            @RequestBody TemplateRequest request) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        templateService.update(id, request, userId);
        return ApiResult.success(templateService.getById(id, userId));
    }

    /**
     * Delete a template.
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteTemplate(@PathVariable Long id) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        templateService.delete(id, userId);
        return ApiResult.success(null);
    }
}

/**
 * Controller for template category management.
 */
@Slf4j
@RestController
@RequestMapping("/api/template-categories")
@RequiredArgsConstructor
class TemplateCategoryController {

    private final TemplateCategoryService templateCategoryService;
    private final QuotaService quotaService;

    /**
     * Get current user's template categories.
     */
    @GetMapping
    public ApiResult<List<TemplateCategory>> getCategories() {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        return ApiResult.success(templateCategoryService.getByUserId(userId));
    }

    /**
     * Create a new category.
     */
    @PostMapping
    public ApiResult<TemplateCategory> createCategory(@RequestBody TemplateCategory category) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        category.setUserId(userId);
        return ApiResult.success(templateCategoryService.create(category));
    }

    /**
     * Update a category.
     */
    @PutMapping("/{id}")
    public ApiResult<Void> updateCategory(@PathVariable Long id, @RequestBody TemplateCategory category) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        category.setId(id);
        templateCategoryService.update(category);
        return ApiResult.success(null);
    }

    /**
     * Delete a category.
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        templateCategoryService.delete(id, userId);
        return ApiResult.success(null);
    }
}
