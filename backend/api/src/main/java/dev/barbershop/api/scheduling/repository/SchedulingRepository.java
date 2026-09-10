package dev.barbershop.api.scheduling.repository;

import dev.barbershop.api.scheduling.entity.Scheduling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SchedulingRepository extends JpaRepository<Scheduling, UUID> {
}
