package com.videosummary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videosummary.entity.TaskResult;
import com.videosummary.entity.TaskResultHistory;
import com.videosummary.mapper.TaskResultHistoryMapper;
import com.videosummary.mapper.TaskResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskResultHistoryService {

    private final TaskResultHistoryMapper historyMapper;
    private final TaskResultMapper taskResultMapper;

    /**
     * 保存编辑历史，保留最近3版
     */
    @Transactional
    public void saveHistory(TaskResult taskResult) {
        // 查询当前版本号
        Integer currentVersion = historyMapper.selectCount(
                new LambdaQueryWrapper<TaskResultHistory>()
                        .eq(TaskResultHistory::getTaskResultId, taskResult.getId())
        );

        // 插入新历史记录
        TaskResultHistory history = TaskResultHistory.builder()
                .taskResultId(taskResult.getId())
                .taskId(taskResult.getTaskId())
                .outputType(taskResult.getOutputType())
                .content(taskResult.getContent())
                .version(currentVersion + 1)
                .build();
        historyMapper.insert(history);

        // 删除超过3版的旧记录
        if (currentVersion + 1 > 3) {
            Integer keepVersion = currentVersion + 1 - 3;
            historyMapper.delete(
                    new LambdaQueryWrapper<TaskResultHistory>()
                            .eq(TaskResultHistory::getTaskResultId, taskResult.getId())
                            .lt(TaskResultHistory::getVersion, keepVersion)
            );
        }
    }

    /**
     * 获取历史版本列表
     */
    public List<TaskResultHistory> getHistory(Long taskResultId) {
        return historyMapper.selectList(
                new LambdaQueryWrapper<TaskResultHistory>()
                        .eq(TaskResultHistory::getTaskResultId, taskResultId)
                        .orderByDesc(TaskResultHistory::getVersion)
        );
    }

    /**
     * 根据 taskId 和 outputType 获取历史版本
     */
    public List<TaskResultHistory> getHistoryByTaskAndType(Long taskId, String outputType) {
        return historyMapper.selectList(
                new LambdaQueryWrapper<TaskResultHistory>()
                        .eq(TaskResultHistory::getTaskId, taskId)
                        .eq(TaskResultHistory::getOutputType, outputType)
                        .orderByDesc(TaskResultHistory::getVersion)
        );
    }
}
