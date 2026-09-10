package dev.barbershop.api.inventorymovement.service;

import dev.barbershop.api.inventorymovement.repository.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryMovementService {
    private final InventoryMovementRepository repository;
}
