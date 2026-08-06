package com.example.mcprice.adapter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Rate limit theo domain (competitor) bang cach cho toi thieu 60_000/requestsPerMinute ms giua 2 lan goi. */
@Component
public class DomainRateLimiter {

    private final ConcurrentHashMap<Long, AtomicLong> lastCallAtMillisByCompetitor = new ConcurrentHashMap<>();

    public void acquire(Long competitorId, int requestsPerMinute) {
        if (requestsPerMinute <= 0) {
            return;
        }
        long minIntervalMillis = 60_000L / requestsPerMinute;
        AtomicLong lastCall = lastCallAtMillisByCompetitor.computeIfAbsent(competitorId, k -> new AtomicLong(0));
        while (true) {
            long now = System.currentTimeMillis();
            long previous = lastCall.get();
            long elapsed = now - previous;
            if (elapsed >= minIntervalMillis) {
                if (lastCall.compareAndSet(previous, now)) {
                    return;
                }
                continue;
            }
            try {
                Thread.sleep(minIntervalMillis - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
