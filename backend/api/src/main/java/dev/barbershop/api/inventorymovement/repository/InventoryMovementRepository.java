package dev.barbershop.api.inventorymovement.repository;

import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
}
