package org.example.appointmentapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {
    @Query("""
            select j.eventId from NotificationJob j
            where (j.status in ('PENDING', 'RETRYING') and j.nextAttemptAt <= :now)
               or (j.status = 'PROCESSING' and j.lockedUntil < :now)
            order by j.nextAttemptAt
            """)
    List<UUID> findClaimableIds(@Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("""
            update NotificationJob j set j.status = 'PROCESSING', j.attempts = j.attempts + 1,
                j.lockedUntil = :leaseExpiry
            where j.eventId = :eventId and
              ((j.status in ('PENDING', 'RETRYING') and j.nextAttemptAt <= :now)
               or (j.status = 'PROCESSING' and j.lockedUntil < :now))
            """)
    int claim(@Param("eventId") UUID eventId, @Param("now") Instant now,
              @Param("leaseExpiry") Instant leaseExpiry);
}
