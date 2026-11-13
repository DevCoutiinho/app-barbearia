package dev.barbershop.api.availability.repository;

import dev.barbershop.api.availability.entity.Availability;
import dev.barbershop.api.barber.entity.Barber;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {

    default List<Availability> findAvailableByBarberAndDay(
            UUID barberId,
            Integer dayWeek
    ) {
        Availability example = Availability.builder()
                .barber(Barber.builder().id(barberId).build())
                .dayWeek(dayWeek)
                .active(true)
                .build();

        return findAll(Example.of(example));
    }
}
