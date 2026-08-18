package com.riigiluup.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * One Telegram ping when the api finishes booting. Doubles as a deploy/restart notification and a
 * live end-to-end check that the alert channel works. In a genuine autoheal restart-loop it repeats
 * — which is itself the signal you want ("why does the api keep restarting?").
 */
@Component
@RequiredArgsConstructor
public class StartupAlert {

    private final TelegramAlertService alert;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        alert.send("🟢 RiigiLuup: api käivitus (deploy või taaskäivitus).");
    }
}
