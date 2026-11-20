package org.example.appointmentapp;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationWorkflowTests {
    private final NotificationJobRepository jobs = mock(NotificationJobRepository.class);
    private final EnterpriseAppointmentClient appointments = mock(EnterpriseAppointmentClient.class);
    private final SmsGateway sms = mock(SmsGateway.class);

    @Test
    void sendsOptedInPatientWithEventIdAsTheProviderIdempotencyKey() {
        UUID eventId = UUID.randomUUID();
        NotificationJob job = job(eventId, AppointmentChangeEvent.ChangeType.APPOINTMENT_CREATED);
        when(jobs.claim(eq(eventId), any(), any())).thenReturn(1);
        when(jobs.findById(eventId)).thenReturn(Optional.of(job));
        when(appointments.getAppointment(job.getAppointmentId())).thenReturn(appointment(true));

        new NotificationJobProcessor(jobs, appointments, sms).claimAndProcess(eventId);

        ArgumentCaptor<UUID> key = ArgumentCaptor.forClass(UUID.class);
        verify(sms).send(eq("+15555550100"), any(), key.capture());
        assertThat(key.getValue()).isEqualTo(eventId);
        assertThat(job.getStatus()).isEqualTo(NotificationJob.Status.SENT);
    }

    @Test
    void suppressesWhenPatientHasNotConsentedToSms() {
        UUID eventId = UUID.randomUUID();
        NotificationJob job = job(eventId, AppointmentChangeEvent.ChangeType.TIME_OR_LOCATION_CHANGED);
        when(jobs.claim(eq(eventId), any(), any())).thenReturn(1);
        when(jobs.findById(eventId)).thenReturn(Optional.of(job));
        when(appointments.getAppointment(job.getAppointmentId())).thenReturn(appointment(false));

        new NotificationJobProcessor(jobs, appointments, sms).claimAndProcess(eventId);

        verify(sms, org.mockito.Mockito.never()).send(any(), any(), any());
        assertThat(job.getStatus()).isEqualTo(NotificationJob.Status.SUPPRESSED);
    }

    @Test
    void doesNotFetchOrNotifyForCancellation() {
        UUID eventId = UUID.randomUUID();
        NotificationJob job = job(eventId, AppointmentChangeEvent.ChangeType.APPOINTMENT_CANCELLED);
        when(jobs.claim(eq(eventId), any(), any())).thenReturn(1);
        when(jobs.findById(eventId)).thenReturn(Optional.of(job));

        new NotificationJobProcessor(jobs, appointments, sms).claimAndProcess(eventId);

        verify(appointments, org.mockito.Mockito.never()).getAppointment(any());
        verify(sms, org.mockito.Mockito.never()).send(any(), any(), any());
        assertThat(job.getStatus()).isEqualTo(NotificationJob.Status.SUPPRESSED);
    }

    @Test
    void retriesDownstreamSmsFailureInsteadOfLosingTheEvent() {
        UUID eventId = UUID.randomUUID();
        NotificationJob job = job(eventId, AppointmentChangeEvent.ChangeType.APPOINTMENT_CREATED);
        when(jobs.claim(eq(eventId), any(), any())).thenReturn(1);
        when(jobs.findById(eventId)).thenReturn(Optional.of(job));
        when(appointments.getAppointment(job.getAppointmentId())).thenReturn(appointment(true));
        doThrow(new IllegalStateException("provider unavailable")).when(sms).send(any(), any(), any());

        new NotificationJobProcessor(jobs, appointments, sms).claimAndProcess(eventId);

        assertThat(job.getStatus()).isEqualTo(NotificationJob.Status.RETRYING);
        assertThat(job.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void duplicateDeliveryIsNotInsertedTwice() {
        NotificationIngestionService ingestion = new NotificationIngestionService(jobs);
        AppointmentChangeEvent event = new AppointmentChangeEvent(
                UUID.randomUUID(), UUID.randomUUID(),
                AppointmentChangeEvent.ChangeType.APPOINTMENT_CREATED, Instant.now());
        doThrow(new DataIntegrityViolationException("duplicate event")).when(jobs).saveAndFlush(any());

        assertThat(ingestion.accept(event)).isFalse();
    }

    private NotificationJob job(UUID eventId, AppointmentChangeEvent.ChangeType changeType) {
        return new NotificationJob(new AppointmentChangeEvent(
                eventId, UUID.randomUUID(), changeType, Instant.now()));
    }

    private EnterpriseAppointmentClient.CurrentAppointment appointment(boolean optedIn) {
        return new EnterpriseAppointmentClient.CurrentAppointment(
                UUID.randomUUID(), optedIn, "+15555550100", Instant.parse("2026-10-05T15:00:00Z"),
                "Downtown Clinic", "100 Main Street");
    }
}
