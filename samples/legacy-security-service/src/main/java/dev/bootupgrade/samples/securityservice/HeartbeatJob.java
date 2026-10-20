package dev.bootupgrade.samples.securityservice;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class HeartbeatJob {

    private final AtomicLong beatCount = new AtomicLong();

    @Scheduled(fixedRate = 60_000)
    public void beat() {
        beatCount.incrementAndGet();
    }

    public long beatCount() {
        return beatCount.get();
    }
}
