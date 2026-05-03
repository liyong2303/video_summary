package com.videosummary.controller;

import com.videosummary.dto.ApiResult;
import com.videosummary.entity.UserPreference;
import com.videosummary.service.QuotaService;
import com.videosummary.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user preference management.
 */
@Slf4j
@RestController
@RequestMapping("/api/preference")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final QuotaService quotaService;

    /**
     * Get current user's preference.
     */
    @GetMapping
    public ApiResult<UserPreference> getPreference() {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.success(userPreferenceService.getDefault());
        }
        return ApiResult.success(userPreferenceService.getOrDefault(userId));
    }

    /**
     * Save or update current user's preference.
     */
    @PostMapping
    public ApiResult<Void> savePreference(@RequestBody UserPreference preference) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) {
            return ApiResult.error(401, "用户未登录");
        }
        preference.setUserId(userId);
        userPreferenceService.saveOrUpdate(preference);
        return ApiResult.success(null);
    }
}
