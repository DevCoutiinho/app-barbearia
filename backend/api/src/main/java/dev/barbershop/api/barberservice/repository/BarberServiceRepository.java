package dev.barbershop.api.barberservice.repository;

import dev.barbershop.api.barberservice.dto.BarberServiceCatalogDTO;
import dev.barbershop.api.barberservice.entity.BarberService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BarberServiceRepository extends JpaRepository<BarberService, UUID> , JpaSpecificationExecutor<BarberService> {

    boolean existsByCategory_Id(UUID categoryId);

    Optional<BarberService> findByIdAndBarberId(UUID id, UUID barberId);

    @Query("""
        SELECT new dev.barbershop.api.barberservice.dto.BarberServiceCatalogDTO(
            b.id, 
            b.name, 
            b.technique, 
            b.price, 
            b.durationTime, 
            barber.id, 
            user.name, 
            user.avatar, 
            user.telephone
        )
        FROM BarberService b
        JOIN b.barber barber
        JOIN barber.user user
        WHERE b.active = true 
        AND user.active = true
    """)
    List<BarberServiceCatalogDTO> findByServicesActive();
}
