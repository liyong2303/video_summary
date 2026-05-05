package com.videosummary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.bilibili.BilibiliVideoService;
import com.videosummary.bilibili.SubtitleNotFoundException;
import com.videosummary.bilibili.dto.SubtitleContent;
import com.videosummary.bilibili.dto.VideoInfo;
import com.videosummary.client.PipelineClient;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videosummary.dto.BatchSubmitResponse;
import com.videosummary.dto.HistoryItem;
import com.videosummary.dto.HistoryPage;
import com.videosummary.dto.SubmitResponse;
import com.videosummary.dto.TaskResponse;
import com.videosummary.entity.Task;
import com.videosummary.entity.TaskResult;
import com.videosummary.entity.TaskResultHistory;
import com.videosummary.mapper.TaskMapper;
import com.videosummary.mapper.TaskResultMapper;
import com.videosummary.mapper.TaskResultHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskResultMapper taskResultMapper;
    private final BilibiliVideoService bilibiliVideoService;
    private final PipelineClient pipelineClient;
    private final QuotaService quotaService;
    private final TaskResultHistoryService taskResultHistoryService;
    private final TaskResultHistoryMapper taskResultHistoryMapper;

    private static final Pattern BV_PATTERN = Pattern.compile("BV[A-Za-z0-9]+");
    private static final Pattern BILIBILI_URL_PATTERN = Pattern.compile("bilibili\\.com/video/(BV[A-Za-z0-9]+)");

    public TaskService(TaskMapper taskMapper, TaskResultMapper taskResultMapper,
                       BilibiliVideoService bilibiliVideoService, PipelineClient pipelineClient,
                       QuotaService quotaService,
                       TaskResultHistoryService taskResultHistoryService,
                       TaskResultHistoryMapper taskResultHistoryMapper) {
        this.taskMapper = taskMapper;
        this.taskResultMapper = taskResultMapper;
        this.bilibiliVideoService = bilibiliVideoService;
        this.pipelineClient = pipelineClient;
        this.quotaService = quotaService;
        this.taskResultHistoryService = taskResultHistoryService;
        this.taskResultHistoryMapper = taskResultHistoryMapper;
    }

    public static String parseBvid(String url) {
        if (url.contains("b23.tv/")) {
            throw new IllegalArgumentException("暂不支持b23.tv短链接，请使用完整BV号");
        }

        Matcher urlMatcher = BILIBILI_URL_PATTERN.matcher(url);
        if (urlMatcher.find()) {
            return urlMatcher.group(1);
        }

        Matcher bvMatcher = BV_PATTERN.matcher(url);
        if (bvMatcher.matches()) {
            return url;
        }

        if (bvMatcher.find()) {
            return bvMatcher.group();
        }

        throw new IllegalArgumentException("无法识别的视频链接，请输入BV号或B站视频链接");
    }

    @Transactional
    public SubmitResponse submit(String url, String style, String length) {
        String bvid = parseBvid(url);

        Long userId = quotaService.getCurrentUserId();
        if (userId == null) userId = 0L;
        final Long finalUserId = userId;

        Task existing = taskMapper.selectOne(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, finalUserId)
                        .eq(Task::getBvid, bvid)
        );

        if (existing != null) {
            return SubmitResponse.builder()
                    .taskId(existing.getId())
                    .bvid(bvid)
                    .videoTitle(existing.getVideoTitle())
                    .videoDuration(existing.getVideoDuration())
                    .coverUrl(existing.getCoverUrl())
                    .status(existing.getStatus())
                    .isExisting(true)
                    .style(style)
                    .length(length)
                    .build();
        }

        if (finalUserId > 0) {
            quotaService.checkAndIncrement(finalUserId);  // 检查+自增在同一事务
        }

        VideoInfo videoInfo = bilibiliVideoService.getVideoInfo(bvid);
        bilibiliVideoService.validateDuration(videoInfo.getDuration());

        Task task = Task.builder()
                .userId(finalUserId)
                .bvid(bvid)
                .cid(videoInfo.getCid())
                .videoTitle(videoInfo.getTitle())
                .videoDuration(videoInfo.getDuration())
                .coverUrl(videoInfo.getCoverUrl())
                .status(Task.Status.PENDING)
                .build();
        taskMapper.insert(task);

        return SubmitResponse.builder()
                .taskId(task.getId())
                .bvid(bvid)
                .videoTitle(videoInfo.getTitle())
                .videoDuration(videoInfo.getDuration())
                .coverUrl(videoInfo.getCoverUrl())
                .status(task.getStatus())
                .isExisting(false)
                .style(style)
                .length(length)
                .build();
    }

    public TaskResponse getTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        // Get result content from task_result table
        String summaryContent = null;
        List<TaskResult> results = taskResultMapper.selectList(
                new LambdaQueryWrapper<TaskResult>().eq(TaskResult::getTaskId, taskId)
        );
        for (TaskResult r : results) {
            if (TaskResult.OutputType.SUMMARY.equals(r.getOutputType())) {
                summaryContent = r.getContent();
                break;
            }
        }

        // Fallback: try reading subtitle file for backward compat
        if (summaryContent == null && Task.Status.COMPLETED.equals(task.getStatus())
                && task.getSubtitleStoragePath() != null) {
            try {
                summaryContent = Files.readString(Path.of(task.getSubtitleStoragePath()));
            } catch (IOException e) {
                log.warn("Failed to read subtitle file: {}", task.getSubtitleStoragePath(), e);
            }
        }

        return TaskResponse.builder()
                .taskId(task.getId())
                .bvid(task.getBvid())
                .videoTitle(task.getVideoTitle())
                .videoDuration(task.getVideoDuration())
                .coverUrl(task.getCoverUrl())
                .status(task.getStatus())
                .subtitleText(summaryContent)
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    public void processTask(Long taskId, String style, String length) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        task.setStatus(Task.Status.PROCESSING);
        taskMapper.updateById(task);

        try {
            // Step 1: Extract subtitles
            Long cid = task.getCid();
            if (cid == null) {
                VideoInfo videoInfo = bilibiliVideoService.getVideoInfo(task.getBvid());
                cid = videoInfo.getCid();
                task.setCid(cid);
            }

            List<SubtitleContent> subtitles = bilibiliVideoService.extractSubtitles(task.getBvid(), cid);
            String subtitleText = bilibiliVideoService.extractSubtitleText(subtitles);

            // Save subtitle to file
            Path subtitleDir = Path.of("data/subtitles");
            Files.createDirectories(subtitleDir);
            Path subtitleFile = subtitleDir.resolve(task.getBvid() + ".txt");
            Files.writeString(subtitleFile, subtitleText);
            task.setSubtitleStoragePath(subtitleFile.toString());

            // Step 2: Call AI pipeline
            PipelineClient.PipelineResult pipelineResult = pipelineClient.execute(
                    String.valueOf(taskId), subtitleText, style, length
            );

            // Step 3: Save pipeline results
            boolean allCompleted = true;
            for (var entry : pipelineResult.getStepResults().entrySet()) {
                PipelineClient.PipelineStepResult stepResult = entry.getValue();
                TaskResult taskResult = TaskResult.builder()
                        .taskId(taskId)
                        .outputType(entry.getKey())
                        .content(stepResult.getContent())
                        .modelUsed("deepseek-chat")
                        .outputTokens(stepResult.getTokensUsed())
                        .status(stepResult.getStatus())
                        .build();
                taskResultMapper.insert(taskResult);

                if (!"completed".equals(stepResult.getStatus())) {
                    allCompleted = false;
                }
            }

            // Step 4: Generate mindmap and script
            PipelineClient.PipelineResult mindmapResult = pipelineClient.executeSingleStep(
                    String.valueOf(taskId), subtitleText, "mindmap", "concise", "standard"
            );
            PipelineClient.PipelineStepResult mindmapStep = mindmapResult.getStepResults().get("mindmap");
            if (mindmapStep == null) {
                throw new RuntimeException("Mindmap step result is null");
            }
            TaskResult mindmapResultTask = TaskResult.builder()
                    .taskId(taskId)
                    .outputType(TaskResult.OutputType.MINDMAP)
                    .content(mindmapStep.getContent())
                    .modelUsed("deepseek-chat")
                    .outputTokens(mindmapStep.getTokensUsed())
                    .status(mindmapStep.getStatus())
                    .build();
            taskResultMapper.insert(mindmapResultTask);
            if (!"completed".equals(mindmapStep.getStatus())) {
                allCompleted = false;
            }

            PipelineClient.PipelineResult scriptResult = pipelineClient.executeSingleStep(
                    String.valueOf(taskId), subtitleText, "script", "professional", "standard"
            );
            PipelineClient.PipelineStepResult scriptStep = scriptResult.getStepResults().get("script");
            if (scriptStep == null) {
                throw new RuntimeException("Script step result is null");
            }
            TaskResult scriptResultTask = TaskResult.builder()
                    .taskId(taskId)
                    .outputType(TaskResult.OutputType.SCRIPT)
                    .content(scriptStep.getContent())
                    .modelUsed("deepseek-chat")
                    .outputTokens(scriptStep.getTokensUsed())
                    .status(scriptStep.getStatus())
                    .build();
            taskResultMapper.insert(scriptResultTask);
            if (!"completed".equals(scriptStep.getStatus())) {
                allCompleted = false;
            }

            // Update task status
            if (allCompleted) {
                task.setStatus(Task.Status.COMPLETED);
            } else {
                task.setStatus(Task.Status.PARTIALLY_COMPLETED);
            }
            task.setCompletedAt(java.time.LocalDateTime.now());

        } catch (SubtitleNotFoundException e) {
            task.setStatus(Task.Status.FAILED);
            task.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            task.setStatus(Task.Status.FAILED);
            task.setErrorMessage("处理失败：" + e.getMessage());
            log.error("Task {} processing failed", taskId, e);
        }

        taskMapper.updateById(task);
    }

    public List<Map<String, Object>> getTaskResults(Long taskId) {
        List<TaskResult> results = taskResultMapper.selectList(
                new LambdaQueryWrapper<TaskResult>().eq(TaskResult::getTaskId, taskId)
        );
        return results.stream().map(r -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("outputType", r.getOutputType());
            map.put("content", r.getContent());
            map.put("status", r.getStatus());
            map.put("modelUsed", r.getModelUsed());
            map.put("tokensUsed", r.getOutputTokens());
            return map;
        }).toList();
    }

    public void cancelTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task != null && (Task.Status.PENDING.equals(task.getStatus())
                || Task.Status.PROCESSING.equals(task.getStatus()))) {
            task.setStatus(Task.Status.CANCELLED);
            taskMapper.updateById(task);
            log.info("Task {} cancelled", taskId);
        }
    }

    public HistoryPage getHistory(int page, int pageSize) {
        Long userId = quotaService.getCurrentUserId();
        if (userId == null) userId = 0L;
        final Long finalUserId = userId;

        Page<Task> taskPage = taskMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, finalUserId)
                        .orderByDesc(Task::getCreatedAt)
        );

        List<HistoryItem> items = taskPage.getRecords().stream().map(task -> {
            List<TaskResult> results = taskResultMapper.selectList(
                    new LambdaQueryWrapper<TaskResult>().eq(TaskResult::getTaskId, task.getId())
            );
            List<String> outputTypes = results.stream()
                    .filter(r -> TaskResult.Status.COMPLETED.equals(r.getStatus()) && r.getContent() != null)
                    .map(TaskResult::getOutputType)
                    .collect(Collectors.toList());

            return HistoryItem.builder()
                    .taskId(task.getId())
                    .bvid(task.getBvid())
                    .videoTitle(task.getVideoTitle())
                    .videoDuration(task.getVideoDuration())
                    .coverUrl(task.getCoverUrl())
                    .status(task.getStatus())
                    .outputTypes(outputTypes)
                    .createdAt(task.getCreatedAt())
                    .completedAt(task.getCompletedAt())
                    .build();
        }).collect(Collectors.toList());

        return HistoryPage.builder()
                .items(items)
                .total(taskPage.getTotal())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    public void regenerate(Long taskId, String outputType) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        // Read subtitle text
        String subtitleText = readSubtitleText(task);
        if (subtitleText == null || subtitleText.isBlank()) {
            throw new IllegalArgumentException("任务没有字幕内容，无法重新生成");
        }

        // Call pipeline for single step
        PipelineClient.PipelineResult pipelineResult = pipelineClient.executeSingleStep(
                String.valueOf(taskId), subtitleText, outputType, "concise", "standard"
        );

        PipelineClient.PipelineStepResult stepResult = pipelineResult.getStepResults().get(outputType);
        if (stepResult == null) {
            throw new IllegalArgumentException("不支持的内容类型: " + outputType);
        }

        // Update or insert result
        TaskResult existing = taskResultMapper.selectOne(
                new LambdaQueryWrapper<TaskResult>()
                        .eq(TaskResult::getTaskId, taskId)
                        .eq(TaskResult::getOutputType, outputType)
        );

        if (existing != null) {
            existing.setContent(stepResult.getContent());
            existing.setStatus(stepResult.getStatus());
            existing.setModelUsed("deepseek-chat");
            existing.setOutputTokens(stepResult.getTokensUsed());
            taskResultMapper.updateById(existing);
        } else {
            TaskResult newResult = TaskResult.builder()
                    .taskId(taskId)
                    .outputType(outputType)
                    .content(stepResult.getContent())
                    .modelUsed("deepseek-chat")
                    .outputTokens(stepResult.getTokensUsed())
                    .status(stepResult.getStatus())
                    .build();
            taskResultMapper.insert(newResult);
        }

        log.info("Task {} regenerated output type {}", taskId, outputType);
    }

    public String exportMarkdown(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        List<TaskResult> results = taskResultMapper.selectList(
                new LambdaQueryWrapper<TaskResult>()
                        .eq(TaskResult::getTaskId, taskId)
                        .eq(TaskResult::getStatus, TaskResult.Status.COMPLETED)
        );

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(task.getVideoTitle() != null ? task.getVideoTitle() : task.getBvid()).append("\n\n");
        sb.append("**BV号**: ").append(task.getBvid()).append("\n\n");

        if (task.getVideoDuration() != null) {
            int min = task.getVideoDuration() / 60;
            int sec = task.getVideoDuration() % 60;
            sb.append("**时长**: ").append(min).append(":").append(String.format("%02d", sec)).append("\n\n");
        }

        if (task.getCreatedAt() != null) {
            sb.append("**生成时间**: ").append(task.getCreatedAt()).append("\n\n");
        }

        sb.append("---\n\n");

        Map<String, String> typeLabels = Map.of(
                "summary", "总结",
                "article", "文章",
                "card", "学习卡片",
                "xiaohongshu", "小红书文案"
        );

        for (TaskResult result : results) {
            String label = typeLabels.getOrDefault(result.getOutputType(), result.getOutputType());
            sb.append("## ").append(label).append("\n\n");
            sb.append(result.getContent()).append("\n\n");
        }

        return sb.toString();
    }

    private String readSubtitleText(Task task) {
        // Try from file first
        if (task.getSubtitleStoragePath() != null) {
            try {
                return Files.readString(Path.of(task.getSubtitleStoragePath()));
            } catch (IOException e) {
                log.warn("Failed to read subtitle file: {}", task.getSubtitleStoragePath(), e);
            }
        }
        // Fallback: try from summary result
        List<TaskResult> results = taskResultMapper.selectList(
                new LambdaQueryWrapper<TaskResult>()
                        .eq(TaskResult::getTaskId, task.getId())
                        .eq(TaskResult::getOutputType, TaskResult.OutputType.SUMMARY)
        );
        if (!results.isEmpty() && results.get(0).getContent() != null) {
            return results.get(0).getContent();
        }
        return null;
    }

    @Transactional
    public void updateResult(Long taskId, String outputType, String content) {
        // 查找现有结果
        TaskResult result = taskResultMapper.selectOne(
                new LambdaQueryWrapper<TaskResult>()
                        .eq(TaskResult::getTaskId, taskId)
                        .eq(TaskResult::getOutputType, outputType)
        );
        if (result == null) {
            throw new IllegalArgumentException("结果不存在");
        }

        // 保存历史
        taskResultHistoryService.saveHistory(result);

        // 更新内容
        result.setContent(content);
        taskResultMapper.updateById(result);

        log.info("Task {} updated result type {}", taskId, outputType);
    }

    @Transactional
    public void rollbackResult(Long taskId, String outputType, Integer version) {
        // 查找历史版本
        TaskResultHistory history = taskResultHistoryMapper.selectOne(
                new LambdaQueryWrapper<TaskResultHistory>()
                        .eq(TaskResultHistory::getTaskId, taskId)
                        .eq(TaskResultHistory::getOutputType, outputType)
                        .eq(TaskResultHistory::getVersion, version)
        );
        if (history == null) {
            throw new IllegalArgumentException("历史版本不存在");
        }

        // 查找当前结果
        TaskResult result = taskResultMapper.selectOne(
                new LambdaQueryWrapper<TaskResult>()
                        .eq(TaskResult::getTaskId, taskId)
                        .eq(TaskResult::getOutputType, outputType)
        );
        if (result == null) {
            throw new IllegalArgumentException("结果不存在");
        }

        // 保存当前版本到历史
        taskResultHistoryService.saveHistory(result);

        // 回滚内容
        result.setContent(history.getContent());
        taskResultMapper.updateById(result);

        log.info("Task {} rolled back result type {} to version {}", taskId, outputType, version);
    }

    @Transactional
    public BatchSubmitResponse batchSubmit(List<String> urls, String style, String length) {
        String batchId = UUID.randomUUID().toString();
        List<BatchSubmitResponse.BatchTaskItem> taskItems = new ArrayList<>();

        for (String url : urls) {
            try {
                SubmitResponse response = submit(url, style, length);
                BatchSubmitResponse.BatchTaskItem item = BatchSubmitResponse.BatchTaskItem.builder()
                        .taskId(response.getTaskId())
                        .url(url)
                        .status(response.getStatus())
                        .build();
                taskItems.add(item);

                if (!response.getIsExisting()) {
                    processTask(response.getTaskId(), style, length);
                    Task updatedTask = taskMapper.selectById(response.getTaskId());
                    item.setStatus(updatedTask.getStatus());
                }
            } catch (Exception e) {
                BatchSubmitResponse.BatchTaskItem item = BatchSubmitResponse.BatchTaskItem.builder()
                        .url(url)
                        .status("failed")
                        .error(e.getMessage())
                        .build();
                taskItems.add(item);
            }
        }

        return BatchSubmitResponse.builder()
                .batchId(batchId)
                .tasks(taskItems)
                .status("completed")
                .build();
    }
}
