package dev.barbershop.api.availability.repository;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.entity.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, UUID> {

    Optional<ScheduleBlock> findByIdAndBarberId(UUID id, UUID barberId);

    @Query("""
            SELECT COUNT(b) > 0
            FROM ScheduleBlock b
            WHERE b.barber.id = :barberId
              AND b.startsAt < :end
              AND b.endsAt > :start
            """)
    boolean existsOverlapping(
            @Param("barberId") UUID barberId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
            SELECT new dev.barbershop.api.availability.domain.TimeInterval(b.startsAt, b.endsAt)
            FROM ScheduleBlock b
            WHERE b.barber.id = :barberId
              AND b.startsAt < :end
              AND b.endsAt > :start
            """)
    List<TimeInterval> findBlockedIntervals(
            @Param("barberId") UUID barberId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
            SELECT b
            FROM ScheduleBlock b
            WHERE b.barber.id = :barberId
              AND b.startsAt < :end
              AND b.endsAt > :start
            ORDER BY b.startsAt
            """)
    List<ScheduleBlock> findAllInPeriod(
            @Param("barberId") UUID barberId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}

