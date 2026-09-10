package dev.barbershop.api.inventorymovement.controller;

import dev.barbershop.api.inventorymovement.service.InventoryMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InventoryMovementController {

    private final InventoryMovementService service;
}
