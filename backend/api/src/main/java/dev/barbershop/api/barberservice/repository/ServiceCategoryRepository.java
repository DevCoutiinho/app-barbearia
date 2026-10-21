package dev.barbershop.api.barberservice.repository;

import dev.barbershop.api.barberservice.entity.ServiceCategory;
import dev.barbershop.api.barberservice.projection.ServiceCategorySummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {

    List<ServiceCategorySummaryProjection> findAllProjectedBy();

    boolean existsByName(String name);
}
