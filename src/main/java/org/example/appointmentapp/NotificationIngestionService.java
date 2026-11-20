package org.example.appointmentapp;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationIngestionService {
    private final NotificationJobRepository jobs;

    NotificationIngestionService(NotificationJobRepository jobs) {
        this.jobs = jobs;
    }

    /**
     * Persists the inbound event and its work item in one database transaction. The event ID is
     * the primary key, so duplicate EventBridge deliveries are harmless across all pods.
     */
    @Transactional
    public boolean accept(AppointmentChangeEvent event) {
        try {
            jobs.saveAndFlush(new NotificationJob(event));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }
}
