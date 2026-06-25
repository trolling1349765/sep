package fpt.capstone.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiter using in-memory ConcurrentHashMap.
 * Each IP gets a bucket of maxAttempts tokens that refills every windowMinutes.
 */
@Service
public class RateLimiterService {

    private final int maxAttempts;
    private final long windowMillis;
    private final ConcurrentHashMap<String, TokenBucket> cache = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${auth.rate-limit.max-attempts}") int maxAttempts,
            @Value("${auth.rate-limit.window-minutes}") int windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMinutes * 60_000L;
    }

    public RateLimitResult tryConsume(String key) {
        TokenBucket bucket = cache.computeIfAbsent(key, k -> new TokenBucket(maxAttempts));
        synchronized (bucket) {
            long now = System.currentTimeMillis();

            // Refill tokens based on elapsed time
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