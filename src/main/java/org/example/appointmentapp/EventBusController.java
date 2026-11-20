package org.example.appointmentapp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Demo EventBridge adapter. In EKS this path is private behind the EventBridge/SQS ingress and
 * authenticated with workload identity; it deliberately accepts identifiers, never patient data.
 */
@RestController
@RequestMapping("/internal/eventbus")
class EventBusController {
    private final NotificationIngestionService ingestion;

    EventBusController(NotificationIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping("/appointment-changes")
    ResponseEntity<Void> receive(@RequestBody EventPayload payload) {
        if (payload.eventId() == null || payload.appointmentId() == null || payload.changeType() == null) {
            return ResponseEntity.badRequest().build();
        }
        boolean accepted = ingestion.accept(new AppointmentChangeEvent(
                payload.eventId(), payload.appointmentId(), payload.changeType(),
                payload.occurredAt() == null ? Instant.now() : payload.occurredAt()));
        return ResponseEntity.status(accepted ? HttpStatus.ACCEPTED : HttpStatus.OK).build();
    }

    record EventPayload(UUID eventId, UUID appointmentId,
                        AppointmentChangeEvent.ChangeType changeType, Instant occurredAt) { }
}
