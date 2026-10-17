package dev.barbershop.api.inventorymovement.service;

import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.inventorymovement.dto.InventoryMovementCreateDTO;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.inventorymovement.enums.InventoryMovementType;
import dev.barbershop.api.inventorymovement.repository.InventoryMovementRepository;
import dev.barbershop.api.product.entity.Product;
import dev.barbershop.api.product.repository.ProductRepository;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryMovementService {

    private final InventoryMovementRepository repository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public InventoryMovement create(InventoryMovementCreateDTO dto) {
        CustomUserDetails principal = (CustomUserDetails)
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Usuário não encontrado"));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Produto não encontrado"));

        if (!product.getActive()) {
            throw new IllegalArgumentException(
                    "Não é possível movimentar um produto inativo");
        }

        BigDecimal quantity = dto.getQuantity();
        BigDecimal currentQuantity = product.getCurrentQuantity();
        BigDecimal newQuantity;

        if (dto.getType() == InventoryMovementType.ENTRY) {
            newQuantity = currentQuantity.add(quantity);
        } else {
            newQuantity = currentQuantity.subtract(quantity);

            if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Estoque insuficiente para essa movimentação");
            }
        }

        Product updatedProduct = Product.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .minimumQuantity(product.getMinimumQuantity())
                .currentQuantity(newQuantity)
                .unitMeasurement(product.getUnitMeasurement())
                .priceCost(product.getPriceCost())
                .active(product.getActive())
                .build();

        productRepository.save(updatedProduct);

        InventoryMovement movement = InventoryMovement.builder()
                .user(user)
                .product(product)
                .quantity(quantity)
                .type(dto.getType())
                .build();

        return repository.save(movement);
    }
}
