package org.example.appointmentapp;

import java.time.Instant;
import java.util.UUID;

public interface EnterpriseAppointmentClient {
    CurrentAppointment getAppointment(UUID appointmentId);

    record CurrentAppointment(
            UUID appointmentId,
            boolean smsOptedIn,
            String phoneNumber,
            Instant startsAt,
            String clinicName,
            String clinicAddress
    ) { }
}
