package org.example.appointmentapp;

import java.time.Instant;
import java.util.UUID;

public record AppointmentChangeEvent(
        UUID eventId,
        UUID appointmentId,
        ChangeType changeType,
        Instant occurredAt
) {
    public enum ChangeType {
        APPOINTMENT_CREATED,
        TIME_OR_LOCATION_CHANGED,
        APPOINTMENT_CANCELLED
    }

    public boolean requiresNotification() {
        return changeType == ChangeType.APPOINTMENT_CREATED
                || changeType == ChangeType.TIME_OR_LOCATION_CHANGED;
    }
}
