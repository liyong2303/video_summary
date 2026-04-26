package com.videosummary.client;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for calling the Python AI pipeline service.
 * Uses X-Internal-Secret header for service-to-service auth.
 */
@Slf4j
@Component
public class PipelineClient {

    private final RestTemplate restTemplate;

    @Value("${app.internal-secret}")
    private String internalSecret;

    @Value("${app.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public PipelineClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Execute the AI pipeline synchronously and return results.
     */
    public PipelineResult execute(String taskId, String subtitleText) {
        String url = aiServiceUrl + "/pipeline/execute";

        Map<String, String> body = new HashMap<>();
        body.put("task_id", taskId);
        body.put("subtitle_text", subtitleText);

        var request = org.springframework.http.RequestEntity
                .post(url)
                .header("Content-Type", "application/json")
                .header("X-Internal-Secret", internalSecret)
                .body(JSONUtil.toJsonStr(body))
                .build();

        try {
            String response = restTemplate.exchange(request, String.class).getBody();
            JSONObject json = JSONUtil.parseObj(response);

            PipelineResult result = new PipelineResult();
            result.setTaskId(json.getStr("task_id"));
            result.setTotalDuration(json.getDouble("total_duration"));

            JSONObject steps = json.getJSONObject("steps");
            for (String stepName : steps.keySet()) {
                JSONObject stepObj = steps.getJSONObject(stepName);
                PipelineStepResult stepResult = new PipelineStepResult();
                stepResult.setName(stepName);
                stepResult.setStatus(stepObj.getStr("status"));
                stepResult.setContent(stepObj.getStr("content"));
                stepResult.setTokensUsed(stepObj.getInt("tokens_used", 0));
                stepResult.setError(stepObj.getStr("error"));
                stepResult.setDuration(stepObj.getDouble("duration", 0.0));
                result.getStepResults().put(stepName, stepResult);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to call AI pipeline: {}", e.getMessage(), e);
            throw new RuntimeException("AI管线调用失败: " + e.getMessage(), e);
        }
    }

    public static class PipelineResult {
        private String taskId;
        private Map<String, PipelineStepResult> stepResults = new HashMap<>();
        private Double totalDuration;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public Map<String, PipelineStepResult> getStepResults() { return stepResults; }
        public void setStepResults(Map<String, PipelineStepResult> stepResults) { this.stepResults = stepResults; }
        public Double getTotalDuration() { return totalDuration; }
        public void setTotalDuration(Double totalDuration) { this.totalDuration = totalDuration; }
    }

    public static class PipelineStepResult {
        private String name;
        private String status;
        private String content;
        private int tokensUsed;
        private String error;
        private double duration;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getTokensUsed() { return tokensUsed; }
        public void setTokensUsed(int tokensUsed) { this.tokensUsed = tokensUsed; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
    }
}
