package dev.barbershop.api.user.mapper;

import dev.barbershop.api.user.dto.UserCreatedDTO;
import dev.barbershop.api.user.dto.UserAdmDTO;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.enums.RoleName;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserCreatedDTO toDTO(User user);

    UserAdmDTO toAdmDTO(User user);

    default RoleName map(Role role) {
        return role.getName();
    }
}