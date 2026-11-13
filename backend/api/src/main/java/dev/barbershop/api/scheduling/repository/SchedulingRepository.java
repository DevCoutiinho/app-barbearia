package dev.barbershop.api.scheduling.repository;

import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SchedulingRepository extends JpaRepository<Scheduling, UUID> {

    @Query("""
        SELECT s
        FROM Scheduling s
        WHERE s.barber.id = :barberId
        AND s.scheduledAt < :end
        AND s.status <> :ignoredStatus
        """)
    List<Scheduling> findBarberBookings(
            @Param("barberId") UUID barberId,
            @Param("end") Instant end,
            @Param("ignoredStatus") SchedulingStatus ignoredStatus
    );

    default List<Scheduling> findClientSchedulings(UUID clientId) {

        Scheduling example = Scheduling.builder()
                .client(Client.builder()
                        .id(clientId)
                        .build())
                .build();

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnorePaths(
                        "id",
                        "barber",
                        "service",
                        "scheduledAt",
                        "status",
                        "finalPrice",
                        "client.user",
                        "client.loyaltyPoints",
                        "client.schedulings"
                );

        return findAll(
                Example.of(example, matcher),
                Sort.by(Sort.Direction.DESC, "scheduledAt")
        );
    }
}
