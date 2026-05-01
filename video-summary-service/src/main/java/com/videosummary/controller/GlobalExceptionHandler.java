package com.videosummary.controller;

import com.videosummary.bilibili.BilibiliApiException;
import com.videosummary.bilibili.SubtitleNotFoundException;
import com.videosummary.bilibili.VideoTooLongException;
import com.videosummary.dto.ApiResult;
import com.videosummary.service.QuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResult.error(400, e.getMessage());
    }

    @ExceptionHandler(SubtitleNotFoundException.class)
    public ApiResult<Void> handleSubtitleNotFound(SubtitleNotFoundException e) {
        return ApiResult.error(404, e.getMessage());
    }

    @ExceptionHandler(VideoTooLongException.class)
    public ApiResult<Void> handleVideoTooLong(VideoTooLongException e) {
        return ApiResult.error(400, e.getMessage());
    }

    @ExceptionHandler(BilibiliApiException.class)
    public ApiResult<Void> handleBilibiliApi(BilibiliApiException e) {
        return ApiResult.error(502, "B站API调用失败：" + e.getMessage());
    }

    @ExceptionHandler(QuotaService.QuotaExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResult<Void> handleQuotaExceeded(QuotaService.QuotaExceededException ex) {
        return ApiResult.error(429, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ApiResult.error(500, "服务器内部错误");
    }
}
