package com.videosummary.bilibili;

public class BilibiliApiException extends RuntimeException {
    public BilibiliApiException(String message) {
        super(message);
    }

    public BilibiliApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
