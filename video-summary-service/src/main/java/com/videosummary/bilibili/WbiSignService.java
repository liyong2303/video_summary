package com.videosummary.bilibili;

/**
 * wbi签名算法 - B站内部接口签名，可能随API更新而变更，本模块独立便于快速适配
 */
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WbiSignService {

    private final WbiKeyService wbiKeyService;

    // Mixin key reorder table - fixed positions for extracting 32 chars from 64-char key
    private static final int[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 16
    };

    public WbiSignService(WbiKeyService wbiKeyService) {
        this.wbiKeyService = wbiKeyService;
    }

    public Map<String, String> sign(Map<String, String> params) {
        // Step 1: Add wts timestamp
        long wts = System.currentTimeMillis() / 1000;
        Map<String, String> signedParams = new LinkedHashMap<>(params);
        signedParams.put("wts", String.valueOf(wts));

        // Step 2: Get keys and build mixin key
        WbiKeyService.WbiKeys keys = wbiKeyService.getWbiKeys();
        String mixinKey = getMixinKey(keys.getImgKey() + keys.getSubKey());

        // Step 3: Sort params by key alphabetically
        List<Map.Entry<String, String>> sorted = signedParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        // Step 4: URL-encode values and concatenate
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, String> entry = sorted.get(i);
            if (i > 0) sb.append('&');
            sb.append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        // Step 5: Append mixin key and calculate MD5
        sb.append(mixinKey);
        String wRid = DigestUtil.md5Hex(sb.toString());

        // Step 6: Add w_rid to params
        signedParams.put("w_rid", wRid);

        log.debug("wbi signed params: wts={}, w_rid={}", wts, wRid);
        return signedParams;
    }

    private String getMixinKey(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int index : MIXIN_KEY_ENC_TAB) {
            if (index < raw.length()) {
                sb.append(raw.charAt(index));
            }
        }
        return sb.toString();
    }
}
