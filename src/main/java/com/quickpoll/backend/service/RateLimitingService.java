package com.quickpoll.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class RateLimitingService {

    private final ConcurrentMap<String, VoteAttempt> voteAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long RATE_WINDOW_SECONDS = 3600;

    public boolean isRateLimited(String deviceFingerprint, Long pollId) {
        String key = deviceFingerprint + ":" + pollId;
        VoteAttempt attempt = voteAttempts.get(key);

        Instant now = Instant.now();

        if (attempt == null) {
            voteAttempts.put(key, new VoteAttempt(now, 1));
            return false;
        }

        if (now.isAfter(attempt.firstAttempt.plusSeconds(RATE_WINDOW_SECONDS))) {
            voteAttempts.put(key, new VoteAttempt(now, 1));
            return false;
        }

        if (attempt.attempts >= MAX_ATTEMPTS) {
            log.warn("Rate limit exceeded for device {} on poll {}", deviceFingerprint, pollId);
            return true;
        }

        attempt.attempts++;
        return false;
    }

    private static class VoteAttempt {
        Instant firstAttempt;
        int attempts;

        VoteAttempt(Instant firstAttempt, int attempts) {
            this.firstAttempt = firstAttempt;
            this.attempts = attempts;
        }
    }
}
