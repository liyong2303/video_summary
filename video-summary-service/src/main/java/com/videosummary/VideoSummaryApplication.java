package com.videosummary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VideoSummaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoSummaryApplication.class, args);
    }
}
