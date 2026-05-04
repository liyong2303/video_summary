package com.videosummary.controller;

import com.videosummary.dto.ApiResult;
import com.videosummary.dto.QuickActionRequest;
import com.videosummary.dto.QuickActionResponse;
import com.videosummary.service.QuotaService;
import com.videosummary.service.QuickActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for quick action management.
 */
@Slf4j
@RestController
@RequestMapping("/api/quick-actions")
@RequiredArgsConstructor
public class QuickActionController {

    private final QuickActionService quickActionService;
    private final QuotaService quotaService;

    /**
     * Get current user's quick actions.
     */
    @GetMapping
    public ApiResult<List<QuickActionResponse>> getQuickActions() {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        return ApiResult.success(quickActionService.getByUserId(userId));
    }

    /**
     * Get quick action by ID.
     */
    @GetMapping("/{id}")
    public ApiResult<QuickActionResponse> getQuickAction(@PathVariable Long id) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        return ApiResult.success(quickActionService.getById(id, userId));
    }

    /**
     * Create a new quick action.
     */
    @PostMapping
    public ApiResult<QuickActionResponse> createQuickAction(@RequestBody QuickActionRequest request) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        QuickAction action = quickActionService.create(request, userId);
        return ApiResult.success(quickActionService.getById(action.getId(), userId));
    }

    /**
     * Update a quick action.
     */
    @PutMapping("/{id}")
    public ApiResult<QuickActionResponse> updateQuickAction(
            @PathVariable Long id,
            @RequestBody QuickActionRequest request) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        quickActionService.update(id, request, userId);
        return ApiResult.success(quickActionService.getById(id, userId));
    }

    /**
     * Delete a quick action.
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteQuickAction(@PathVariable Long id) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        quickActionService.delete(id, userId);
        return ApiResult.success(null);
    }
}
