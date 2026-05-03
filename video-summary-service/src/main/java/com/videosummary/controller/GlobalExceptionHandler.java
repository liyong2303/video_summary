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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResult.error(400, e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(SubtitleNotFoundException.class)
    public ApiResult<Void> handleSubtitleNotFound(SubtitleNotFoundException e) {
        return ApiResult.error(404, e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(VideoTooLongException.class)
    public ApiResult<Void> handleVideoTooLong(VideoTooLongException e) {
        return ApiResult.error(400, e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(BilibiliApiException.class)
    public ApiResult<Void> handleBilibiliApi(BilibiliApiException e) {
        return ApiResult.error(502, "B站API调用失败：" + e.getMessage());
    }

    @ExceptionHandler(QuotaService.QuotaExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResult<Void> handleQuotaExceeded(QuotaService.QuotaExceededException ex) {
        return ApiResult.error(429, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ApiResult<Void> handleRequestTimeout(ResourceAccessException e) {
        return ApiResult.error(408, "AI 服务请求超时，请稍后重试");
    }

    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ApiResult<Void> handleTimeout(TimeoutException e) {
        return ApiResult.error(408, "处理超时，请稍后重试");
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ApiResult<Void> handleDataAccess(DataAccessException e) {
        log.error("Database error", e);
        return ApiResult.error(503, "数据库服务暂时不可用");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(cn.dev33.satoken.exception.NotLoginException.class)
    public ApiResult<Void> handleNotLogin(NotLoginException e) {
        return ApiResult.error(401, "请先登录");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ApiResult.error(500, "服务器内部错误");
    }
}
