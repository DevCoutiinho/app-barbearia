package dev.barbershop.api.barberservice.repository;

import dev.barbershop.api.barberservice.entity.BarberService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BarberServiceRepository extends JpaRepository<BarberService, UUID> {
}
