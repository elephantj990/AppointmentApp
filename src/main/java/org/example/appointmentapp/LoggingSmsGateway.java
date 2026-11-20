package org.example.appointmentapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class LoggingSmsGateway implements SmsGateway {
    private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public void send(String phoneNumber, String message, UUID idempotencyKey) {
        // The actual provider request must receive idempotencyKey; never log recipient or body.
        log.info("SMS accepted by demo provider: eventId={}", idempotencyKey);
    }
}
