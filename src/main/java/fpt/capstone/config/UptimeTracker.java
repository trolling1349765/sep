package fpt.capstone.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Holds the moment the application became ready; the admin dashboard exposes
 * it so the FE can derive uptime.
 */
@Component
public class UptimeTracker {

    private volatile Instant startedAt = Instant.now();

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        this.startedAt = Instant.now();
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
