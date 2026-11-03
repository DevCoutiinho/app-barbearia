package dev.barbershop.api.product.mapper;

import dev.barbershop.api.product.dto.ProductCreateDTO;
import dev.barbershop.api.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "currentQuantity", source = "initialQuantity")
    @Mapping(target = "inventoryMovements", ignore = true)
    Product toEntity(ProductCreateDTO dto);
}
