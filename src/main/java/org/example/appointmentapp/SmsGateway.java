package org.example.appointmentapp;

import java.util.UUID;

public interface SmsGateway {
    void send(String phoneNumber, String message, UUID idempotencyKey);
}
