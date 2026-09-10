package dev.barbershop.api.barberservice.repository;

import dev.barbershop.api.barberservice.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {
}
