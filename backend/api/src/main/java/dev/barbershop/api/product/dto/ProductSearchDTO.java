package dev.barbershop.api.product.dto;

import dev.barbershop.api.product.enums.UnitMeasurement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchDTO {

    private String name;
    private String description;
    private UnitMeasurement unitMeasurement;
    private Boolean active;
}
