package dev.barbershop.api.product.specification;


import dev.barbershop.api.product.dto.ProductFilterDTO;
import dev.barbershop.api.product.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;


public class ProductSpecification {

    public static Specification<Product> findByFilter(ProductFilterDTO dto) {
        return ((root, query, builder) -> {

            Predicate predicate = builder.conjunction();

            if (dto.name() != null && !dto.name().isBlank()) {
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("name")), "%" + dto.name().toLowerCase(Locale.ROOT) + "%"));
            }

            if (dto.description() != null && !dto.description().isBlank()) {
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("description")), "%" + dto.description().toLowerCase(Locale.ROOT) + "%"));
            }

            if (dto.unitMeasurement() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("unitMeasurement"), dto.unitMeasurement()));
            }

            if (dto.priceCost() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("priceCost"), dto.priceCost()));

            }

            if (dto.active() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), dto.active()));
            }

            return predicate;
        });
    }
}
