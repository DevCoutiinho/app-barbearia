package dev.barbershop.api.auth.authentication.mapper;

import dev.barbershop.api.user.dto.UserCreatedDTO;
import dev.barbershop.api.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    UserCreatedDTO toDTO(User user);
}
