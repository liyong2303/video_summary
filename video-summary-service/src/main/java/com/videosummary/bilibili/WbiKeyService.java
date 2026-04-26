package com.videosummary.bilibili;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class WbiKeyService {

    private final RestTemplate restTemplate;

    @Value("${app.bilibili.cookie}")
    private String cookie;

    private volatile WbiKeys cachedKeys;
    private volatile long lastFetchTime = 0;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

    public WbiKeyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WbiKeys getWbiKeys() {
        if (cachedKeys == null || System.currentTimeMillis() - lastFetchTime > CACHE_TTL_MS) {
            refreshKeys();
        }
        return cachedKeys;
    }

    @Scheduled(fixedRate = 600000)
    public void scheduledRefresh() {
        refreshKeys();
    }

    private synchronized void refreshKeys() {
        try {
            String url = "https://api.bilibili.com/x/web-interface/nav";
            var request = org.springframework.http.RequestEntity.get(url)
                    .header("Cookie", cookie)
                    .build();
            String response = restTemplate.exchange(request, String.class).getBody();

            JSONObject json = JSONUtil.parseObj(response);
            JSONObject data = json.getJSONObject("data");
            JSONObject wbiImg = data.getJSONObject("wbi_img");

            String imgUrl = wbiImg.getStr("img_url");
            String subUrl = wbiImg.getStr("sub_url");

            String imgKey = extractKey(imgUrl);
            String subKey = extractKey(subUrl);

            cachedKeys = new WbiKeys(imgKey, subKey);
            lastFetchTime = System.currentTimeMillis();
            log.info("wbi keys refreshed, imgKey={}..., subKey={}...",
                    imgKey.substring(0, Math.min(8, imgKey.length())),
                    subKey.substring(0, Math.min(8, subKey.length())));
        } catch (Exception e) {
            log.error("Failed to refresh wbi keys", e);
            if (cachedKeys == null) {
                throw new BilibiliApiException("获取wbi密钥失败: " + e.getMessage(), e);
            }
        }
    }

    private String extractKey(String url) {
        // Remove prefix and .png extension
        // e.g. https://i0.hdslb.com/bfs/wbi/xxx.png -> xxx
        String key = url;
        int lastSlash = key.lastIndexOf('/');
        if (lastSlash >= 0) {
            key = key.substring(lastSlash + 1);
        }
        if (key.endsWith(".png")) {
            key = key.substring(0, key.length() - 4);
        }
        return key;
    }

    @Data
    public static class WbiKeys {
        private final String imgKey;
        private final String subKey;
    }
}
