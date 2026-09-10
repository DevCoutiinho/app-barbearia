package dev.barbershop.api.availability.repository;

import dev.barbershop.api.availability.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {
}
