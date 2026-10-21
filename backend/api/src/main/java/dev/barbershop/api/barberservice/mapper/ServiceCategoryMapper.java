package dev.barbershop.api.barberservice.mapper;

import dev.barbershop.api.barberservice.dto.servicecategory.ServiceCategoryCreateDTO;
import dev.barbershop.api.barberservice.dto.servicecategory.ServiceCategoryResponseDTO;
import dev.barbershop.api.barberservice.entity.ServiceCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceCategoryMapper {

    ServiceCategory toEntity(ServiceCategoryCreateDTO dto);

    ServiceCategoryResponseDTO toCategoryResponseDTO(ServiceCategory serviceCategory);
}
