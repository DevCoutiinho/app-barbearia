package dev.barbershop.api.barber.repository;

import dev.barbershop.api.barber.entity.Barber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BarberRepository extends JpaRepository<Barber, UUID> {

    Optional<Barber> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Barber b WHERE b.id = :id")
    Optional<Barber> findByIdWithLock(@Param("id") UUID id);
}
