package dev.barbershop.api.user.mapper;

import dev.barbershop.api.user.dto.UserCreatedDTO;
import dev.barbershop.api.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserCreatedDTO toDTO(User user);

}
