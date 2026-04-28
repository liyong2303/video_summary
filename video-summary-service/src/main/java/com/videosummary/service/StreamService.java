package com.videosummary.service;

import com.videosummary.bilibili.BilibiliVideoService;
import com.videosummary.bilibili.dto.SubtitleContent;
import com.videosummary.bilibili.dto.VideoInfo;
import com.videosummary.entity.Task;
import com.videosummary.entity.TaskResult;
import com.videosummary.mapper.TaskMapper;
import com.videosummary.mapper.TaskResultMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class StreamService {

    private final TaskMapper taskMapper;
    private final TaskResultMapper taskResultMapper;
    private final BilibiliVideoService bilibiliVideoService;

    @Value("${app.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${app.internal-secret}")
    private String internalSecret;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Long, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> cancelledTasks = new ConcurrentHashMap<>();

    public StreamService(TaskMapper taskMapper, TaskResultMapper taskResultMapper,
                         BilibiliVideoService bilibiliVideoService) {
        this.taskMapper = taskMapper;
        this.taskResultMapper = taskResultMapper;
        this.bilibiliVideoService = bilibiliVideoService;
    }

    public SseEmitter createStream(Long taskId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout
        activeEmitters.put(taskId, emitter);

        emitter.onCompletion(() -> activeEmitters.remove(taskId));
        emitter.onTimeout(() -> activeEmitters.remove(taskId));
        emitter.onError(e -> activeEmitters.remove(taskId));

        // If lastEventId provided, send already-completed results first
        if (lastEventId != null && !lastEventId.isEmpty()) {
            sendCompletedResults(taskId, emitter);
        }

        executor.submit(() -> processAndStream(taskId, emitter));
        return emitter;
    }

    public void cancelTask(Long taskId) {
        cancelledTasks.put(taskId, true);
        SseEmitter emitter = activeEmitters.remove(taskId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    private void sendCompletedResults(Long taskId, SseEmitter emitter) {
        try {
            List<TaskResult> results = taskResultMapper.selectList(
                    new LambdaQueryWrapper<TaskResult>().eq(TaskResult::getTaskId, taskId)
            );
            for (TaskResult r : results) {
                if ("completed".equals(r.getStatus())) {
                    emitter.send(SseEmitter.event()
                            .name("step_complete")
                            .data(Map.of(
                                    "step", r.getOutputType(),
                                    "status", r.getStatus(),
                                    "content", r.getContent() != null ? r.getContent() : ""
                            )));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send completed results for reconnection: {}", e.getMessage());
        }
    }

    private void processAndStream(Long taskId, SseEmitter emitter) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            sendError(emitter, "任务不存在");
            return;
        }

        try {
            // Update task to processing
            task.setStatus(Task.Status.PROCESSING);
            taskMapper.updateById(task);

            // Step 1: Extract subtitles
            sendEvent(emitter, "step_start", Map.of("step", "extract"));
            Long cid = task.getCid();
            if (cid == null) {
                VideoInfo videoInfo = bilibiliVideoService.getVideoInfo(task.getBvid());
                cid = videoInfo.getCid();
                task.setCid(cid);
            }

            List<SubtitleContent> subtitles = bilibiliVideoService.extractSubtitles(task.getBvid(), cid);
            String subtitleText = bilibiliVideoService.extractSubtitleText(subtitles);

            // Save subtitle
            java.nio.file.Path subtitleDir = java.nio.file.Path.of("data/subtitles");
            java.nio.file.Files.createDirectories(subtitleDir);
            java.nio.file.Path subtitleFile = subtitleDir.resolve(task.getBvid() + ".txt");
            java.nio.file.Files.writeString(subtitleFile, subtitleText);
            task.setSubtitleStoragePath(subtitleFile.toString());
            taskMapper.updateById(task);

            sendEvent(emitter, "step_complete", Map.of("step", "extract", "status", "completed"));

            // Check if cancelled
            if (Boolean.TRUE.equals(cancelledTasks.remove(taskId))) {
                task.setStatus(Task.Status.CANCELLED);
                taskMapper.updateById(task);
                emitter.complete();
                return;
            }

            // Step 2: Call Python AI pipeline with SSE streaming
            streamPythonPipeline(taskId, subtitleText, emitter);

            // Update task to completed
            task.setStatus(Task.Status.COMPLETED);
            task.setCompletedAt(java.time.LocalDateTime.now());
            taskMapper.updateById(task);

            sendEvent(emitter, "pipeline_complete", Map.of("status", "completed"));

        } catch (Exception e) {
            log.error("Stream processing failed for task {}", taskId, e);
            task.setStatus(Task.Status.FAILED);
            task.setErrorMessage(e.getMessage());
            taskMapper.updateById(task);
            sendError(emitter, e.getMessage());
        } finally {
            activeEmitters.remove(taskId);
            try { emitter.complete(); } catch (Exception ignored) {}
        }
    }

    private void streamPythonPipeline(Long taskId, String subtitleText, SseEmitter emitter) {
        try {
            URL url = new URL(aiServiceUrl + "/pipeline/execute/stream");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Internal-Secret", internalSecret);
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);

            // Send request body
            String body = String.format("{\"task_id\":\"%d\",\"subtitle_text\":%s}",
                    taskId, jsonEscape(subtitleText));
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

            // Read SSE stream
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                if (Boolean.TRUE.equals(cancelledTasks.get(taskId))) {
                    reader.close();
                    conn.disconnect();
                    return;
                }
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    // Forward to Vue client
                    emitter.send(SseEmitter.event().data(data));

                    // Persist step_complete results
                    if (data.contains("\"step_complete\"")) {
                        persistStepResult(taskId, data);
                    }
                }
            }
            reader.close();
            conn.disconnect();

        } catch (Exception e) {
            log.error("Failed to stream Python pipeline for task {}", taskId, e);
            throw new RuntimeException("AI管线流式调用失败: " + e.getMessage(), e);
        }
    }

    private void persistStepResult(Long taskId, String eventData) {
        try {
            // Parse the event data to extract step info
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> event = mapper.readValue(eventData, Map.class);
            String step = (String) event.get("step");

            // Fetch the full content from Python (non-streaming) for persistence
            // For now, we'll save what we have from the step_complete event
            // The full content is persisted by Python via callback in the real architecture

            // For Phase 3: save step result from non-streaming endpoint
            // This is a simplification - in production, Python would callback to Java
            TaskResult existing = taskResultMapper.selectOne(
                    new LambdaQueryWrapper<TaskResult>()
                            .eq(TaskResult::getTaskId, taskId)
                            .eq(TaskResult::getOutputType, step)
            );
            // Skip if already exists (from non-streaming path)
            if (existing == null) {
                TaskResult result = TaskResult.builder()
                        .taskId(taskId)
                        .outputType(step)
                        .content("")  // Content saved via separate fetch
                        .modelUsed("deepseek-chat")
                        .status("completed")
                        .build();
                taskResultMapper.insert(result);
            }
        } catch (Exception e) {
            log.warn("Failed to persist step result: {}", e.getMessage());
        }
    }

    private void sendEvent(SseEmitter emitter, String type, Map<String, Object> data) {
        try {
            Map<String, Object> event = new java.util.HashMap<>(data);
            event.put("type", type);
            emitter.send(SseEmitter.event().data(event));
        } catch (Exception e) {
            log.warn("Failed to send SSE event: {}", e.getMessage());
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(Map.of("type", "error", "message", message)));
        } catch (Exception ignored) {}
    }

    private String jsonEscape(String text) {
        if (text == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
