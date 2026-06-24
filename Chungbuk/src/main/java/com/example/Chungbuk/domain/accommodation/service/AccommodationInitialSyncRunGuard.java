package com.example.Chungbuk.domain.accommodation.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class AccommodationInitialSyncRunGuard {

    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean tryStart() {
        return running.compareAndSet(false, true);
    }

    public void finish() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }
}
