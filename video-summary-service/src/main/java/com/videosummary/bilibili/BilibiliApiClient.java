package com.videosummary.bilibili;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class BilibiliApiClient {

    private final RestTemplate restTemplate;
    private final WbiSignService wbiSignService;

    @Value("${app.bilibili.cookie}")
    private String cookie;

    public BilibiliApiClient(RestTemplate restTemplate, WbiSignService wbiSignService) {
        this.restTemplate = restTemplate;
        this.wbiSignService = wbiSignService;
    }

    public JSONObject getVideoInfo(String bvid) {
        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
        var request = org.springframework.http.RequestEntity.get(url)
                .header("Cookie", cookie)
                .build();
        String response = restTemplate.exchange(request, String.class).getBody();

        JSONObject json = JSONUtil.parseObj(response);
        int code = json.getInt("code", -1);
        if (code != 0) {
            String message = json.getStr("message", "未知错误");
            throw new BilibiliApiException("获取视频信息失败: " + message);
        }
        return json.getJSONObject("data");
    }

    public JSONObject getPlayerInfo(String bvid, Long cid) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("bvid", bvid);
        params.put("cid", String.valueOf(cid));
        params.put("qn", "64");
        params.put("fnver", "0");
        params.put("fnval", "16");
        params.put("fourk", "1");

        Map<String, String> signedParams = wbiSignService.sign(params);

        StringBuilder urlBuilder = new StringBuilder("https://api.bilibili.com/x/player/wbi/v2?");
        signedParams.forEach((k, v) -> urlBuilder.append(k).append('=').append(v).append('&'));
        String url = urlBuilder.substring(0, urlBuilder.length() - 1);

        var request = org.springframework.http.RequestEntity.get(url)
                .header("Cookie", cookie)
                .build();
        String response = restTemplate.exchange(request, String.class).getBody();

        JSONObject json = JSONUtil.parseObj(response);
        int code = json.getInt("code", -1);
        if (code != 0) {
            String message = json.getStr("message", "未知错误");
            throw new BilibiliApiException("获取播放器信息失败: " + message);
        }
        return json.getJSONObject("data");
    }
}
