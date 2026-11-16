package dev.barbershop.api.scheduling.specification;

import dev.barbershop.api.scheduling.dto.BarberSchedulingFilterDTO;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SchedulingSpecification {

    /**
     * O histórico do barbeiro exibe ESTRITAMENTE serviços finalizados ou cancelados.
     */
    private static final Set<SchedulingStatus> ALLOWED_HISTORY_STATUSES = Set.of(
            SchedulingStatus.COMPLETED,
            SchedulingStatus.CANCELED
    );

    public static Specification<Scheduling> forBarberWithFilters(UUID barberId, BarberSchedulingFilterDTO dto, ZoneId zone) {
        return (root, query, builder) -> {
            Predicate predicate = builder.conjunction();

            // Sempre restringe os atendimentos ao barbeiro autenticado
            predicate = builder.and(predicate, builder.equal(root.get("barber").get("id"), barberId));

            // Regra de Negócio: Histórico deve conter APENAS cancelados e finalizados
            if (dto != null && dto.status() != null) {
                if (ALLOWED_HISTORY_STATUSES.contains(dto.status())) {
                    predicate = builder.and(predicate, builder.equal(root.get("status"), dto.status()));
                } else {
                    predicate = builder.and(predicate, builder.disjunction());
                }
            } else {
                predicate = builder.and(predicate, root.get("status").in(ALLOWED_HISTORY_STATUSES));
            }

            if (dto == null) {
                return predicate;
            }

            // Filtro parcial e case-insensitive por nome do cliente
            if (dto.clientName() != null && !dto.clientName().isBlank()) {
                Join<Object, Object> clientJoin = root.join("client", JoinType.INNER);
                Join<Object, Object> userJoin = clientJoin.join("user", JoinType.INNER);
                predicate = builder.and(predicate, builder.like(
                        builder.lower(userJoin.get("name")),
                        "%" + dto.clientName().trim().toLowerCase(Locale.ROOT) + "%"
                ));
            }

            // Filtro por intervalo de data (interpreta startDate às 00:00:00 e endDate às 23:59:59.999999999 no fuso da barbearia)
            if (dto.startDate() != null) {
                Instant startInstant = dto.startDate().atStartOfDay(zone).toInstant();
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("scheduledAt"), startInstant));
            }

            if (dto.endDate() != null) {
                Instant endInstant = dto.endDate().atTime(LocalTime.MAX).atZone(zone).toInstant();
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("scheduledAt"), endInstant));
            }

            return predicate;
        };
    }
}


