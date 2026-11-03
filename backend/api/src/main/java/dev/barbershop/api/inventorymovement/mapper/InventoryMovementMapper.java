package dev.barbershop.api.inventorymovement.mapper;

import dev.barbershop.api.inventorymovement.dto.InventoryMovementEntryDTO;
import dev.barbershop.api.inventorymovement.dto.InventoryMovementOutputDTO;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface InventoryMovementMapper {

    InventoryMovement toEntity(InventoryMovementEntryDTO dto);

    @Mapping(target = "user", ignore = true)
    InventoryMovement toEntity(InventoryMovementOutputDTO dto);
}
