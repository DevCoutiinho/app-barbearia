package dev.barbershop.api.barberservice.mapper;

import dev.barbershop.api.barberservice.dto.BarberServiceCreateDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceResponseDTO;
import dev.barbershop.api.barberservice.entity.BarberService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {ServiceCategoryMapper.class, ServiceImageMapper.class})
public interface BarberServiceMapper {

    @Mapping(target = "category", ignore = true)
    BarberService toEntity(BarberServiceCreateDTO dto);

    BarberServiceResponseDTO toResponseDTO(BarberService service);
}
