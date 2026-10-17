package dev.barbershop.api.product.dto;

import dev.barbershop.api.product.enums.UnitMeasurement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductCreateDTO {
  
    @NotBlank
    @Size(max = 100) 
    private String name;

    private String description;

    @NotNull 
    @DecimalMin("0.00")
    private BigDecimal minimumQuantity;

    @NotNull 
    @DecimalMin("0.00")
    private BigDecimal initialQuantity;

    @NotNull 
    private UnitMeasurement unitMeasurement;

    @NotNull 
    @DecimalMin("0.00")
    private BigDecimal priceCost;

    @NotNull 
    private Boolean active;
}
