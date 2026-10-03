package dev.barbershop.api.user.admin.specification;

import dev.barbershop.api.user.admin.dto.UserFilterDTO;
import dev.barbershop.api.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public class UserSpecification {

    public static Specification<User> findByFilter(UserFilterDTO dto) {
        return ((from,query ,builder) -> {

            Predicate predicate = builder.conjunction();

            if (dto.name() != null && !dto.name().isEmpty()) {
                predicate = builder.and(predicate, builder.like(builder.lower(from.get("name")), "%" + dto.name().toLowerCase(Locale.ROOT) + "%"));
            }

            if (dto.email() != null && !dto.email().isEmpty()) {
                predicate = builder.and(predicate, builder.like(builder.lower(from.get("email")), "%" + dto.email().toLowerCase(Locale.ROOT) + "%"));
            }

            if (dto.active() != null) {
                predicate = builder.and(predicate, builder.equal(from.get("active"), dto.active()));
            }

            return predicate;
        });
    }
}
