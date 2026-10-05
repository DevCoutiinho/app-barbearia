package dev.barbershop.api.auth.authorization.mapper;

import dev.barbershop.api.auth.authorization.dto.PermissionCreateDTO;
import dev.barbershop.api.auth.authorization.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    @Mapping(target = "roles", ignore = true)
    Permission toEntity(PermissionCreateDTO dto);
}
