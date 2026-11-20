package org.example.appointmentapp;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class NotificationWorker {
    private final NotificationJobRepository jobs;
    private final NotificationJobProcessor processor;

    NotificationWorker(NotificationJobRepository jobs, NotificationJobProcessor processor) {
        this.jobs = jobs;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${notification.worker.delay-ms:1000}")
    public void processAvailable() {
        List<UUID> candidates = jobs.findClaimableIds(Instant.now(), PageRequest.of(0, 25));
        candidates.forEach(processor::claimAndProcess);
    }
}
