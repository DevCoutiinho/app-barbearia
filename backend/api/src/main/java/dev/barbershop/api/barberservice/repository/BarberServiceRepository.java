package dev.barbershop.api.barberservice.repository;

import dev.barbershop.api.barberservice.entity.BarberService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface BarberServiceRepository extends JpaRepository<BarberService, UUID> , JpaSpecificationExecutor<BarberService> {

    boolean existsByCategory_Id(UUID categoryId);

    Optional<BarberService> findByIdAndBarberId(UUID id, UUID barberId);
}
