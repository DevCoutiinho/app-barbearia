package dev.barbershop.api.barber.repository;

import dev.barbershop.api.barber.entity.Barber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BarberRepository extends JpaRepository<Barber, UUID> {
}
