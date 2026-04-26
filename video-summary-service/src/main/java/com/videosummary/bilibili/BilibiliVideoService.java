package com.videosummary.bilibili;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.videosummary.bilibili.dto.SubtitleContent;
import com.videosummary.bilibili.dto.VideoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BilibiliVideoService {

    private final BilibiliApiClient bilibiliApiClient;
    private final RestTemplate restTemplate;

    @Value("${app.video.duration-limit-minutes:30}")
    private Integer durationLimitMinutes;

    public BilibiliVideoService(BilibiliApiClient bilibiliApiClient, RestTemplate restTemplate) {
        this.bilibiliApiClient = bilibiliApiClient;
        this.restTemplate = restTemplate;
    }

    public VideoInfo getVideoInfo(String bvid) {
        JSONObject data = bilibiliApiClient.getVideoInfo(bvid);

        JSONObject owner = data.getJSONObject("owner");
        JSONObject page = data.getJSONArray("pages").getJSONObject(0);

        return VideoInfo.builder()
                .bvid(bvid)
                .title(data.getStr("title"))
                .duration(data.getInt("duration"))
                .coverUrl(data.getStr("pic"))
                .cid(page.getLong("cid"))
                .owner(owner != null ? owner.getStr("name") : "未知")
                .build();
    }

    public List<SubtitleContent> extractSubtitles(String bvid, Long cid) {
        JSONObject data = bilibiliApiClient.getPlayerInfo(bvid, cid);

        JSONObject subtitleObj = data.getJSONObject("subtitle");
        if (subtitleObj == null) {
            throw new SubtitleNotFoundException("该视频没有字幕，目前仅支持有字幕的视频");
        }

        JSONArray subtitles = subtitleObj.getJSONArray("subtitles");
        if (subtitles == null || subtitles.isEmpty()) {
            throw new SubtitleNotFoundException("该视频没有字幕，目前仅支持有字幕的视频");
        }

        // Find the first Chinese subtitle
        String subtitleUrl = null;
        for (int i = 0; i < subtitles.size(); i++) {
            JSONObject sub = subtitles.getJSONObject(i);
            String langKey = sub.getStr("lang_key", "");
            if (langKey.contains("zh")) {
                subtitleUrl = sub.getStr("subtitle_url");
                break;
            }
        }

        // Fallback to first subtitle if no Chinese found
        if (subtitleUrl == null) {
            subtitleUrl = subtitles.getJSONObject(0).getStr("subtitle_url");
        }

        // Fix URL if it starts with //
        if (subtitleUrl.startsWith("//")) {
            subtitleUrl = "https:" + subtitleUrl;
        }

        // Fetch subtitle JSON
        String subtitleJson = restTemplate.getForObject(subtitleUrl, String.class);
        JSONArray body = JSONUtil.parseObj(subtitleJson).getJSONArray("body");

        List<SubtitleContent> result = new ArrayList<>();
        for (int i = 0; i < body.size(); i++) {
            JSONObject item = body.getJSONObject(i);
            result.add(new SubtitleContent(
                    item.getDouble("from"),
                    item.getDouble("to"),
                    item.getStr("content")
            ));
        }

        log.info("Extracted {} subtitle segments for {}", result.size(), bvid);
        return result;
    }

    public void validateDuration(Integer durationSeconds) {
        if (durationSeconds != null && durationSeconds > durationLimitMinutes * 60) {
            throw new VideoTooLongException(
                    "视频时长超过" + durationLimitMinutes + "分钟限制，免费用户仅支持"
                            + durationLimitMinutes + "分钟以内的视频");
        }
    }

    public String extractSubtitleText(List<SubtitleContent> subtitles) {
        return subtitles.stream()
                .map(SubtitleContent::getContent)
                .collect(Collectors.joining("\n"));
    }
}
