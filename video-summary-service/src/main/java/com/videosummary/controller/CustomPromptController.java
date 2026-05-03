package com.videosummary.controller;

import com.videosummary.dto.ApiResult;
import com.videosummary.entity.CustomPrompt;
import com.videosummary.service.QuotaService;
import com.videosummary.service.CustomPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for custom prompt management.
 */
@Slf4j
@RestController
@RequestMapping("/api/custom-prompt")
@RequiredArgsConstructor
public class CustomPromptController {

    private final CustomPromptService customPromptService;
    private final QuotaService quotaService;

    /**
     * Get current user's custom prompts.
     */
    @GetMapping
    public ApiResult<List<CustomPrompt>> getCustomPrompts() {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        return ApiResult.success(customPromptService.getByUserId(userId));
    }

    /**
     * Create a new custom prompt.
     */
    @PostMapping
    public ApiResult<CustomPrompt> createCustomPrompt(@RequestBody CustomPrompt prompt) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        prompt.setUserId(userId);
        customPromptService.save(prompt);
        return ApiResult.success(prompt);
    }

    /**
     * Update a custom prompt.
     */
    @PutMapping("/{id}")
    public ApiResult<CustomPrompt> updateCustomPrompt(
            @PathVariable Long id,
            @RequestBody CustomPrompt prompt) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        // Verify ownership
        CustomPrompt existing = customPromptService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return ApiResult.error(403, "无权修改此Prompt");
        }
        prompt.setId(id);
        prompt.setUserId(userId);
        customPromptService.update(prompt);
        return ApiResult.success(prompt);
    }

    /**
     * Delete a custom prompt.
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteCustomPrompt(@PathVariable Long id) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        customPromptService.delete(id, userId);
        return ApiResult.success(null);
    }

    /**
     * Set a custom prompt as default for its output type.
     */
    @PostMapping("/{id}/set-default")
    public ApiResult<Void> setDefault(
            @PathVariable Long id,
            @RequestParam String outputType) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        customPromptService.setDefault(id, userId, outputType);
        return ApiResult.success(null);
    }
}
