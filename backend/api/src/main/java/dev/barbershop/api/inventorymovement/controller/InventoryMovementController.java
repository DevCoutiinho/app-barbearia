package dev.barbershop.api.inventorymovement.controller;

import dev.barbershop.api.inventorymovement.dto.InventoryMovementCreateDTO;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.inventorymovement.repository.InventoryMovementRepository;
import dev.barbershop.api.inventorymovement.service.InventoryMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/stock/movements")
@RequiredArgsConstructor
public class InventoryMovementController {

    private final InventoryMovementService service;
    private final InventoryMovementRepository repository;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('INVENTORY_CREATE', 'INVENTORY_UPDATE')")
    public InventoryMovement create(
            @Valid @RequestBody InventoryMovementCreateDTO dto) {
        return service.create(dto);
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public List<InventoryMovement> findByProduct(
            @PathVariable UUID productId) {
        return repository.findAllByProductIdOrderByCreatedAtDesc(productId);
    }
}
