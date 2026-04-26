package com.videosummary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.bilibili.BilibiliVideoService;
import com.videosummary.bilibili.SubtitleNotFoundException;
import com.videosummary.bilibili.dto.SubtitleContent;
import com.videosummary.bilibili.dto.VideoInfo;
import com.videosummary.dto.SubmitResponse;
import com.videosummary.dto.TaskResponse;
import com.videosummary.entity.Task;
import com.videosummary.mapper.TaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final BilibiliVideoService bilibiliVideoService;

    private static final Pattern BV_PATTERN = Pattern.compile("BV[A-Za-z0-9]+");
    private static final Pattern BILIBILI_URL_PATTERN = Pattern.compile("bilibili\\.com/video/(BV[A-Za-z0-9]+)");

    public TaskService(TaskMapper taskMapper, BilibiliVideoService bilibiliVideoService) {
        this.taskMapper = taskMapper;
        this.bilibiliVideoService = bilibiliVideoService;
    }

    public static String parseBvid(String url) {
        // Check for b23.tv short URLs
        if (url.contains("b23.tv/")) {
            throw new IllegalArgumentException("暂不支持b23.tv短链接，请使用完整BV号");
        }

        // Try full URL pattern first
        Matcher urlMatcher = BILIBILI_URL_PATTERN.matcher(url);
        if (urlMatcher.find()) {
            return urlMatcher.group(1);
        }

        // Try bare BV号
        Matcher bvMatcher = BV_PATTERN.matcher(url);
        if (bvMatcher.matches()) {
            return url;
        }

        // Try finding BV号 anywhere in string
        if (bvMatcher.find()) {
            return bvMatcher.group();
        }

        throw new IllegalArgumentException("无法识别的视频链接，请输入BV号或B站视频链接");
    }

    public SubmitResponse submit(String url) {
        String bvid = parseBvid(url);

        // Check for existing task (deduplication)
        Task existing = taskMapper.selectOne(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, 0L)
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
                    .build();
        }

        // Get video info from Bilibili
        VideoInfo videoInfo = bilibiliVideoService.getVideoInfo(bvid);
        bilibiliVideoService.validateDuration(videoInfo.getDuration());

        // Create task
        Task task = Task.builder()
                .userId(0L)
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
                .build();
    }

    public TaskResponse getTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        String subtitleText = null;
        if (Task.Status.COMPLETED.equals(task.getStatus()) && task.getSubtitleStoragePath() != null) {
            try {
                subtitleText = Files.readString(Path.of(task.getSubtitleStoragePath()));
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
                .subtitleText(subtitleText)
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    public void processTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        task.setStatus(Task.Status.PROCESSING);
        taskMapper.updateById(task);

        try {
            // Re-fetch video info if cid is missing
            Long cid = task.getCid();
            if (cid == null) {
                VideoInfo videoInfo = bilibiliVideoService.getVideoInfo(task.getBvid());
                cid = videoInfo.getCid();
                task.setCid(cid);
            }

            // Extract subtitles
            List<SubtitleContent> subtitles = bilibiliVideoService.extractSubtitles(task.getBvid(), cid);
            String subtitleText = bilibiliVideoService.extractSubtitleText(subtitles);

            // Save subtitle to file
            Path subtitleDir = Path.of("data/subtitles");
            Files.createDirectories(subtitleDir);
            Path subtitleFile = subtitleDir.resolve(task.getBvid() + ".txt");
            Files.writeString(subtitleFile, subtitleText);

            task.setSubtitleStoragePath(subtitleFile.toString());
            task.setStatus(Task.Status.COMPLETED);
            task.setCompletedAt(java.time.LocalDateTime.now());
        } catch (SubtitleNotFoundException e) {
            task.setStatus(Task.Status.FAILED);
            task.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            task.setStatus(Task.Status.FAILED);
            task.setErrorMessage("处理失败：" + e.getMessage());
        }

        taskMapper.updateById(task);
    }
}
