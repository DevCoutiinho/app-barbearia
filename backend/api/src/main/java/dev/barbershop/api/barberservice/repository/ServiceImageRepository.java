package dev.barbershop.api.barberservice.repository;

import dev.barbershop.api.barberservice.entity.ServiceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceImageRepository extends JpaRepository<ServiceImage, UUID> {
}
