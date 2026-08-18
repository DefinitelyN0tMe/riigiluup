package com.riigiluup.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Fire-and-forget ops alerts to the owner's personal Telegram bot (import failures, startup, etc.).
 * Reads TELEGRAM_BOT_TOKEN + TELEGRAM_ALERT_CHAT_ID from the environment (server .env, gitignored);
 * blank/unset = disabled (so dev and CI never try to send). {@link #send} NEVER throws — a failed
 * notification must not break the caller (an import that is already failing must not also crash on
 * the alert).
 */
@Slf4j
@Service
public class TelegramAlertService {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private final String token;
    private final String chatId;
    private final boolean enabled;
    private final RestClient rest = RestClient.builder()
            .requestFactory(timeoutFactory())
            .build();

    public TelegramAlertService(
            @Value("${TELEGRAM_BOT_TOKEN:}") String token,
            @Value("${TELEGRAM_ALERT_CHAT_ID:}") String chatId) {
        this.token = token == null ? "" : token.trim();
        this.chatId = chatId == null ? "" : chatId.trim();
        this.enabled = !this.token.isBlank() && !this.chatId.isBlank();
        if (!enabled) {
            log.info("Telegram alerts disabled (TELEGRAM_BOT_TOKEN / TELEGRAM_ALERT_CHAT_ID not set)");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Send an ops message to the owner's Telegram. Best-effort: logs and swallows any failure. */
    public void send(String text) {
        if (!enabled) return;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("chat_id", chatId);
            form.add("text", text);
            form.add("disable_web_page_preview", "true");
            rest.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", token)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Never propagate: the alert is a side-channel, not part of the caller's contract.
            log.warn("Telegram alert send failed: {}", e.getMessage());
        }
    }

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(CONNECT_TIMEOUT_MS);
        f.setReadTimeout(READ_TIMEOUT_MS);
        return f;
    }
}
