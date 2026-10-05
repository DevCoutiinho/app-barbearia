package dev.barbershop.api.auth.authorization.mapper;

import dev.barbershop.api.auth.authorization.dto.RoleCreateDTO;
import dev.barbershop.api.auth.authorization.dto.RoleResponseDTO;
import dev.barbershop.api.auth.authorization.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = PermissionMapper.class )
public interface RoleMapper {

    @Mapping(target = "permissions", ignore = true)
    Role toEntity(RoleCreateDTO dto);

    RoleResponseDTO toRoleResponseDTO(Role role);
}
