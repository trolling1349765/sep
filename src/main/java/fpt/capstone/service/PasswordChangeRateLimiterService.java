package fpt.capstone.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user rate limiter for password change attempts.
 * Each userId gets a token bucket with password.change.max-attempts per window.
 */
@Service
public class PasswordChangeRateLimiterService {

    private final int maxAttempts;
    private final long windowMillis;
    private final ConcurrentHashMap<String, TokenBucket> cache = new ConcurrentHashMap<>();

    public PasswordChangeRateLimiterService(
            @Value("${auth.password.change.max-attempts}") int maxAttempts,
            @Value("${auth.password.change.window-minutes}") int windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMinutes * 60_000L;
    }

    public RateLimitResult tryConsume(String userId) {
        TokenBucket bucket = cache.computeIfAbsent(userId, k -> new TokenBucket(maxAttempts));
        synchronized (bucket) {
            long now = System.currentTimeMillis();
            long elapsed = now - bucket.lastRefillTimestamp;
            if (elapsed > 0) {
                long tokensToAdd = elapsed * maxAttempts / windowMillis;
                if (tokensToAdd > 0) {
                    bucket.tokens = Math.min(maxAttempts, bucket.tokens + tokensToAdd);
                    bucket.lastRefillTimestamp = now;
                }
            }

            if (bucket.tokens > 0) {
                bucket.tokens--;
                return new RateLimitResult(true, 0);
            }

            long waitMillis = windowMillis - (now - bucket.lastRefillTimestamp);
            long retryAfterSeconds = Math.max(1, (waitMillis + 999) / 1000);
            return new RateLimitResult(false, retryAfterSeconds);
        }
    }

    public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
    }

    private static class TokenBucket {
        long tokens;
        long lastRefillTimestamp;

        TokenBucket(long initialTokens) {
            this.tokens = initialTokens;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }
    }
}