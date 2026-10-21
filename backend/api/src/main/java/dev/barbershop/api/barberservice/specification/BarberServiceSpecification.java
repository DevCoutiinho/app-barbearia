package dev.barbershop.api.barberservice.specification;

import dev.barbershop.api.barberservice.dto.BarberServiceFilterDTO;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.barberservice.entity.ServiceCategory;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

public class BarberServiceSpecification {

    public static Specification<BarberService> findByFilter(UUID userId, BarberServiceFilterDTO dto) {
        return ((from, query, builder) -> {

            Predicate predicate = builder.conjunction();

            if (dto.name() != null && !dto.name().isEmpty()) {
                predicate = builder.and(predicate, builder.like(builder.lower(from.get("name")), "%" + dto.name().toLowerCase(Locale.ROOT) + "%"));
            }

            if (dto.technique() != null) {
                predicate = builder.and(predicate, builder.equal(from.get("technique"), dto.technique()));
            }

            if (dto.active() != null) {
                predicate = builder.and(predicate, builder.equal(from.get("active"), dto.active()));
            }

            if (dto.minPrice() != null && dto.maxPrice() != null) {
                predicate = builder.and(predicate, builder.between(from.get("price"), dto.minPrice(), dto.maxPrice()));
            } else if (dto.minPrice() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(from.get("price"), dto.minPrice()));
            } else if (dto.maxPrice() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(from.get("price"), dto.maxPrice()));
            }

            if (dto.categoryId() != null) {
                Join<BarberService, ServiceCategory> joinCategory = from.join("category", JoinType.INNER);
                predicate = builder.and(predicate, builder.equal(joinCategory.get("id"), dto.categoryId()));
            }

            if (userId != null) {
                predicate = builder.and(predicate, builder.equal(from.get("barber").get("user").get("id"), userId));
            }

            return predicate;
        });
    }
}
