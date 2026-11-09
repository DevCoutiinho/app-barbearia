package dev.barbershop.api.availability.repository;

import dev.barbershop.api.availability.entity.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, UUID> {

    List<WorkingHour> findByBarberIdOrderByDayOfWeekAscStartTimeAsc(UUID barberId);

    List<WorkingHour> findByBarberIdAndDayOfWeekOrderByStartTimeAsc(UUID barberId, DayOfWeek dayOfWeek);

    /**
     * Remove a grade inteira do barbeiro. Atua apenas sobre barber_working_hours:
     * nenhum agendamento é tocado.
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM WorkingHour w WHERE w.barber.id = :barberId")
    void deleteAllByBarberId(@Param("barberId") UUID barberId);
}
