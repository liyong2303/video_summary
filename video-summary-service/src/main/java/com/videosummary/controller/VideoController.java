package com.videosummary.controller;

import com.videosummary.dto.ApiResult;
import com.videosummary.dto.SubmitRequest;
import com.videosummary.dto.SubmitResponse;
import com.videosummary.dto.TaskResponse;
import com.videosummary.service.TaskService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final TaskService taskService;

    public VideoController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/submit")
    public ApiResult<SubmitResponse> submit(@RequestBody @Valid SubmitRequest request) {
        SubmitResponse response = taskService.submit(request.getUrl());
        // After creating the task, process it synchronously (Phase 1 only)
        if (!response.getIsExisting()) {
            taskService.processTask(response.getTaskId());
            // Re-fetch to get updated status
            TaskResponse updated = taskService.getTask(response.getTaskId());
            response.setStatus(updated.getStatus());
            response.setVideoTitle(updated.getVideoTitle());
        }
        return ApiResult.success(response);
    }

    @GetMapping("/{taskId}")
    public ApiResult<TaskResponse> getTask(@PathVariable Long taskId) {
        return ApiResult.success(taskService.getTask(taskId));
    }
}
