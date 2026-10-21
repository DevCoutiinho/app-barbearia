package dev.barbershop.api.barberservice.mapper;

import dev.barbershop.api.barberservice.dto.serviceimage.ServiceImageResponseDTO;
import dev.barbershop.api.barberservice.entity.ServiceImage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceImageMapper {

    ServiceImageResponseDTO toImageResponseDTO(ServiceImage image);
}
