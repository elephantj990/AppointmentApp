package org.example.appointmentapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
class NotificationJobProcessor {
    private static final Logger log = LoggerFactory.getLogger(NotificationJobProcessor.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final DateTimeFormatter APPOINTMENT_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final NotificationJobRepository jobs;
    private final EnterpriseAppointmentClient appointments;
    private final SmsGateway sms;

    NotificationJobProcessor(NotificationJobRepository jobs, EnterpriseAppointmentClient appointments, SmsGateway sms) {
        this.jobs = jobs;
        this.appointments = appointments;
        this.sms = sms;
    }

    @Transactional
    public void claimAndProcess(UUID eventId) {
        Instant now = Instant.now();
        if (jobs.claim(eventId, now, now.plusSeconds(60)) != 1) {
            return;
        }

        NotificationJob job = jobs.findById(eventId).orElseThrow();
        if (!job.getChangeType().equals(AppointmentChangeEvent.ChangeType.APPOINTMENT_CREATED)
                && !job.getChangeType().equals(AppointmentChangeEvent.ChangeType.TIME_OR_LOCATION_CHANGED)) {
            job.suppressed();
            log.info("Notification suppressed for non-notifying event: eventId={}", eventId);
            return;
        }

        try {
            EnterpriseAppointmentClient.CurrentAppointment appointment =
                    appointments.getAppointment(job.getAppointmentId());
            if (!appointment.smsOptedIn() || appointment.phoneNumber().isBlank()) {
                job.suppressed();
                log.info("Notification suppressed by contact preference: eventId={}", eventId);
                return;
            }

            sms.send(appointment.phoneNumber(), renderMessage(appointment), eventId);
            job.sent();
            log.info("Appointment notification sent: eventId={}", eventId);
        } catch (RuntimeException downstreamFailure) {
            if (job.getAttempts() >= MAX_ATTEMPTS) {
                job.failed();
                log.error("Appointment notification permanently failed: eventId={}, failureType={}",
                        eventId, downstreamFailure.getClass().getSimpleName());
            } else {
                job.retryAt(now.plusSeconds(retryDelaySeconds(job.getAttempts())));
                log.warn("Appointment notification will retry: eventId={}, attempt={}, failureType={}",
                        eventId, job.getAttempts(), downstreamFailure.getClass().getSimpleName());
            }
        }
    }

    private String renderMessage(EnterpriseAppointmentClient.CurrentAppointment appointment) {
        return "Your appointment at " + appointment.clinicName() + " is scheduled for "
                + APPOINTMENT_TIME.format(appointment.startsAt()) + " at " + appointment.clinicAddress()
                + ". Reply STOP to opt out.";
    }

    private long retryDelaySeconds(int attempts) {
        int retryNumber = Math.max(1, attempts);
        return Math.min(3600, 30L * (1L << Math.min(retryNumber - 1, 6)));
    }
}
