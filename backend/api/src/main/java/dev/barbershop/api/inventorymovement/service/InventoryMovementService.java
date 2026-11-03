package dev.barbershop.api.inventorymovement.service;

import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.inventorymovement.dto.InventoryMovementEntryDTO;
import dev.barbershop.api.inventorymovement.dto.InventoryMovementOutputDTO;
import dev.barbershop.api.inventorymovement.dto.InventoryMovementResponseDTO;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.inventorymovement.mapper.InventoryMovementMapper;
import dev.barbershop.api.inventorymovement.repository.InventoryMovementRepository;
import dev.barbershop.api.product.entity.Product;
import dev.barbershop.api.product.repository.ProductRepository;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryMovementService {

    private final InventoryMovementRepository repository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryMovementMapper mapper;

    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public List<InventoryMovementResponseDTO> findHistoryByProductId(UUID productId) {
        return repository.findHistoryByProductId(productId);
    }

    @Transactional
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public void recordIncomingQuantity(UUID userId, UUID productId, InventoryMovementEntryDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum usuario com esse Id"));

        if (!user.isActive()) {
            throw new IllegalStateException("Usuário inativo não pode registrar movimentações de estoque");
        }

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum produto com esse Id"));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException("Produto inativo não pode receber movimentações de estoque");
        }

        product.addStock(dto.quantity());

        InventoryMovement movement = mapper.toEntity(dto);
        movement.registerEntry();
        movement.setUser(user);
        movement.setProduct(product);

        repository.save(movement);
    }

    @Transactional
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public void recordOutputQuantity(UUID productId,InventoryMovementOutputDTO dto) {

        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum usuario com esse Id"));

        if (!user.isActive()) {
            throw new IllegalStateException("Usuário inativo não pode registrar movimentações de estoque");
        }

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum produto com esse Id"));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException("Produto inativo não pode sofrer movimentações de estoque");
        }

        product.removeStock(dto.quantity());

        InventoryMovement movement = mapper.toEntity(dto);
        movement.registerOutput();
        movement.setUser(user);
        movement.setProduct(product);

        repository.save(movement);
    }
}
