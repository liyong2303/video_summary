package com.videosummary.bilibili;

public class VideoTooLongException extends RuntimeException {
    public VideoTooLongException(String message) {
        super(message);
    }
}
