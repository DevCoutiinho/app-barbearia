package dev.barbershop.api.scheduling.repository;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SchedulingRepository extends JpaRepository<Scheduling, UUID>, JpaSpecificationExecutor<Scheduling> {

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

    /**
     * Verifica sobreposição ignorando um ID de agendamento específico (utilizado durante remarcação).
     */
    @Query("""
            SELECT COUNT(s) > 0
            FROM Scheduling s
            WHERE s.barber.id = :barberId
              AND s.id <> :excludedId
              AND s.status IN :statuses
              AND s.scheduledAt < :end
              AND s.endsAt > :start
            """)
    boolean existsOverlappingExcept(
            @Param("barberId") UUID barberId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("statuses") Collection<SchedulingStatus> statuses,
            @Param("excludedId") UUID excludedId
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

    /**
     * Impede que o mesmo cliente tenha dois agendamentos simultâneos (mesmo com barbeiros diferentes).
     */
    @Query("""
            SELECT COUNT(s) > 0
            FROM Scheduling s
            WHERE s.client.id = :clientId
              AND s.status IN :statuses
              AND s.scheduledAt < :end
              AND s.endsAt > :start
            """)
    boolean existsOverlappingForClient(
            @Param("clientId") UUID clientId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("statuses") Collection<SchedulingStatus> statuses
    );

    /**
     * Verifica sobreposição na agenda do cliente ignorando um agendamento específico (durante remarcação).
     */
    @Query("""
            SELECT COUNT(s) > 0
            FROM Scheduling s
            WHERE s.client.id = :clientId
              AND s.id <> :excludedId
              AND s.status IN :statuses
              AND s.scheduledAt < :end
              AND s.endsAt > :start
            """)
    boolean existsOverlappingForClientExcept(
            @Param("clientId") UUID clientId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("statuses") Collection<SchedulingStatus> statuses,
            @Param("excludedId") UUID excludedId
    );

    List<Scheduling> findByClientUserIdOrderByScheduledAtDesc(UUID userId);

    @Query("""
            SELECT s
            FROM Scheduling s
            JOIN FETCH s.client c
            JOIN FETCH c.user u
            JOIN FETCH s.service sv
            WHERE s.barber.id = :barberId
              AND s.scheduledAt >= :start
              AND s.scheduledAt <= :end
            ORDER BY s.scheduledAt ASC
            """)
    List<Scheduling> findDailyByBarber(
            @Param("barberId") UUID barberId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
