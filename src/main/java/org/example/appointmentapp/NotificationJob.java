package org.example.appointmentapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_job")
public class NotificationJob {
    public enum Status { PENDING, PROCESSING, SENT, SUPPRESSED, RETRYING, FAILED }

    @Id
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AppointmentChangeEvent.ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    private Instant nextAttemptAt;
    private Instant lockedUntil;
    private Instant completedAt;

    protected NotificationJob() {
    }

    NotificationJob(AppointmentChangeEvent event) {
        this.eventId = event.eventId();
        this.appointmentId = event.appointmentId();
        this.changeType = event.changeType();
        this.status = Status.PENDING;
        this.nextAttemptAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public UUID getAppointmentId() { return appointmentId; }
    public AppointmentChangeEvent.ChangeType getChangeType() { return changeType; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }

    void claim(Instant leaseExpiry) {
        status = Status.PROCESSING;
        attempts++;
        lockedUntil = leaseExpiry;
    }

    void sent() {
        status = Status.SENT;
        completedAt = Instant.now();
        lockedUntil = null;
    }

    void suppressed() {
        status = Status.SUPPRESSED;
        completedAt = Instant.now();
        lockedUntil = null;
    }

    void retryAt(Instant nextAttempt) {
        status = Status.RETRYING;
        nextAttemptAt = nextAttempt;
        lockedUntil = null;
    }

    void failed() {
        status = Status.FAILED;
        completedAt = Instant.now();
        lockedUntil = null;
    }
}
