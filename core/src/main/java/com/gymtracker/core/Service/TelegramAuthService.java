package com.gymtracker.core.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramAuthService {
    private static final long MAX_AUTH_AGE_SECONDS = 24 * 60 * 60;

    private final String botToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramAuthService(@Value("${telegram.bot.token}") String botToken) {
        this.botToken = botToken;
    }

    public Long validateAndGetTelegramId(String initData) {
        if (initData == null || initData.isBlank()) {
            throw new RuntimeException("Telegram initData is missing");
        }

        Map<String, String> params = parseQuery(initData);
        String receivedHash = params.remove("hash");
        if (receivedHash == null || receivedHash.isBlank()) {
            throw new RuntimeException("Telegram initData hash is missing");
        }

        String dataCheckString = buildDataCheckString(params);
        String calculatedHash = hmacHex(hmacBytes("WebAppData".getBytes(StandardCharsets.UTF_8), botToken), dataCheckString);
        if (!constantTimeEquals(calculatedHash, receivedHash)) {
            throw new RuntimeException("Telegram initData hash is invalid");
        }

        validateAuthDate(params.get("auth_date"));
        return extractTelegramId(params.get("user"));
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int separatorIndex = pair.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }

            String key = URLDecoder.decode(pair.substring(0, separatorIndex), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separatorIndex + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private String buildDataCheckString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        keys.sort(String::compareTo);

        List<String> lines = new ArrayList<>();
        for (String key : keys) {
            lines.add(key + "=" + params.get(key));
        }
        return String.join("\n", lines);
    }

    private void validateAuthDate(String authDateRaw) {
        if (authDateRaw == null || authDateRaw.isBlank()) {
            throw new RuntimeException("Telegram auth_date is missing");
        }

        long authDate = Long.parseLong(authDateRaw);
        long now = Instant.now().getEpochSecond();
        if (now - authDate > MAX_AUTH_AGE_SECONDS) {
            throw new RuntimeException("Telegram initData is expired");
        }
    }

    private Long extractTelegramId(String userJson) {
        if (userJson == null || userJson.isBlank()) {
            throw new RuntimeException("Telegram user is missing");
        }

        try {
            JsonNode user = objectMapper.readTree(userJson);
            return user.get("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Telegram user is invalid", e);
        }
    }

    private byte[] hmacBytes(byte[] key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(key, "HmacSHA256"));
            return hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Cannot calculate Telegram signature", e);
        }
    }

    private String hmacHex(byte[] key, String data) {
        byte[] bytes = hmacBytes(key, data);
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private boolean constantTimeEquals(String first, String second) {
        if (first == null || second == null || first.length() != second.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < first.length(); i++) {
            result |= first.charAt(i) ^ second.charAt(i);
        }
        return result == 0;
    }
}
