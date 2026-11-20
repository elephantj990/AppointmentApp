package org.example.appointmentapp;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
class DemoEnterpriseAppointmentClient implements EnterpriseAppointmentClient {
    @Override
    public CurrentAppointment getAppointment(UUID appointmentId) {
        // Demo only: production implementation calls Enterprise Appointment Service using workload identity.
        return new CurrentAppointment(appointmentId, true, "+15555550100",
                Instant.now().plus(2, ChronoUnit.DAYS), "Downtown Clinic", "100 Main Street");
    }
}
