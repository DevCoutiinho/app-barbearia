package dev.barbershop.api.scheduling.repository;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SchedulingRepository extends JpaRepository<Scheduling, UUID> {

    /**
     * Existe algum agendamento que ocupa a agenda e se sobrepõe (de qualquer forma) ao período?
     * Sobreposição de intervalos semiabertos: a.start < b.end AND a.end > b.start.
     */
    @Query("""
            SELECT COUNT(s) > 0
            FROM Scheduling s
            WHERE s.barber.id = :barberId
              AND s.status IN :statuses
              AND s.scheduledAt < :end
              AND s.endsAt > :start
            """)
    boolean existsOverlapping(
            @Param("barberId") UUID barberId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("statuses") Collection<SchedulingStatus> statuses
    );

    @Query("""
            SELECT new dev.barbershop.api.availability.domain.TimeInterval(s.scheduledAt, s.endsAt)
            FROM Scheduling s
            WHERE s.barber.id = :barberId
              AND s.status IN :statuses
              AND s.scheduledAt < :end
              AND s.endsAt > :start
            """)
    List<TimeInterval> findOccupiedIntervals(
            @Param("barberId") UUID barberId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("statuses") Collection<SchedulingStatus> statuses
    );
}
