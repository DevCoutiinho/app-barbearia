package dev.barbershop.api.user.mapper;

import dev.barbershop.api.user.admin.dto.UserAdminResponseDTO;
import dev.barbershop.api.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserAdminResponseDTO toAdminResponseDTO(User user);
}
