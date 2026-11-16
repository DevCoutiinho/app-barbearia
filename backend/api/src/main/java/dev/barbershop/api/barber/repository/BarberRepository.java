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

    /**
     * Trava a linha do barbeiro (SELECT ... FOR UPDATE) até o fim da transação.
     * <p>
     * Funciona como um "mutex da agenda": toda operação que pode criar conflito na agenda
     * (novo agendamento, novo bloqueio, troca da grade) adquire este lock antes de validar e
     * gravar. Assim, duas requisições concorrentes para o MESMO barbeiro são executadas em
     * fila, e a segunda sempre enxerga o que a primeira gravou. Barbeiros diferentes não se
     * bloqueiam entre si.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Barber b WHERE b.id = :id")
    Optional<Barber> findByIdForUpdate(@Param("id") UUID id);
}
