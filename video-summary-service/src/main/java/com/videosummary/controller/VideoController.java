package com.videosummary.controller;

import com.videosummary.dto.ApiResult;
import com.videosummary.dto.HistoryPage;
import com.videosummary.dto.SubmitRequest;
import com.videosummary.dto.SubmitResponse;
import com.videosummary.dto.TaskResponse;
import com.videosummary.entity.TaskResultHistory;
import com.videosummary.service.StreamService;
import com.videosummary.service.TaskService;
import com.videosummary.service.TaskResultHistoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final TaskService taskService;
    private final StreamService streamService;
    private final TaskResultHistoryService taskResultHistoryService;

    public VideoController(TaskService taskService, StreamService streamService,
                          TaskResultHistoryService taskResultHistoryService) {
        this.taskService = taskService;
        this.streamService = streamService;
        this.taskResultHistoryService = taskResultHistoryService;
    }

    @PostMapping("/submit")
    public ApiResult<SubmitResponse> submit(@RequestBody @Valid SubmitRequest request) {
        SubmitResponse response = taskService.submit(request.getUrl());
        // Process synchronously for non-SSE clients
        if (!response.getIsExisting()) {
            taskService.processTask(response.getTaskId());
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

    @GetMapping("/{taskId}/results")
    public ApiResult<List<Map<String, Object>>> getTaskResults(@PathVariable Long taskId) {
        return ApiResult.success(taskService.getTaskResults(taskId));
    }

    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTask(@PathVariable Long taskId,
                                 @RequestParam(required = false) String lastEventId) {
        log.info("SSE stream for task {}, lastEventId={}", taskId, lastEventId);
        return streamService.createStream(taskId, lastEventId);
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResult<Void> cancelTask(@PathVariable Long taskId) {
        streamService.cancelTask(taskId);
        taskService.cancelTask(taskId);
        return ApiResult.success(null);
    }

    @GetMapping("/history")
    public ApiResult<HistoryPage> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.success(taskService.getHistory(page, pageSize));
    }

    @PostMapping("/{taskId}/regenerate/{outputType}")
    public ApiResult<Void> regenerate(@PathVariable Long taskId, @PathVariable String outputType) {
        taskService.regenerate(taskId, outputType);
        return ApiResult.success(null);
    }

    @GetMapping("/{taskId}/export")
    public ResponseEntity<byte[]> exportMarkdown(@PathVariable Long taskId) {
        String markdown = taskService.exportMarkdown(taskId);
        String filename = URLEncoder.encode("视频摘要.md", StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(markdown.getBytes(StandardCharsets.UTF_8));
    }

    @PutMapping("/{taskId}/result/{outputType}")
    public ApiResult<Void> updateResult(
            @PathVariable Long taskId,
            @PathVariable String outputType,
            @RequestBody Map<String, String> body) {
        taskService.updateResult(taskId, outputType, body.get("content"));
        return ApiResult.success(null);
    }

    @GetMapping("/{taskId}/result/{outputType}/history")
    public ApiResult<List<TaskResultHistory>> getResultHistory(
            @PathVariable Long taskId,
            @PathVariable String outputType) {
        return ApiResult.success(taskResultHistoryService.getHistoryByTaskAndType(taskId, outputType));
    }

    @PostMapping("/{taskId}/result/{outputType}/rollback/{version}")
    public ApiResult<Void> rollbackResult(
            @PathVariable Long taskId,
            @PathVariable String outputType,
            @PathVariable Integer version) {
        taskService.rollbackResult(taskId, outputType, version);
        return ApiResult.success(null);
    }
}
