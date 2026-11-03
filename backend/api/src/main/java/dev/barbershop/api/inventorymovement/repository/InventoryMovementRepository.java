package dev.barbershop.api.inventorymovement.repository;

import dev.barbershop.api.inventorymovement.dto.InventoryMovementResponseDTO;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, UUID> {

    @Query("""
        SELECT new dev.barbershop.api.inventorymovement.dto.InventoryMovementResponseDTO(
            m.id, m.createdAt, u.id, u.name, m.quantity, m.type
        ) 
        FROM InventoryMovement m 
        JOIN m.user u 
        WHERE m.product.id = :productId 
        ORDER BY m.createdAt DESC
    """)
    List<InventoryMovementResponseDTO> findHistoryByProductId(@Param("productId") UUID productId);

}
