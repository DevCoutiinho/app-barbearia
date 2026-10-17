package dev.barbershop.api.product.service;

import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.inventorymovement.enums.InventoryMovementType;
import dev.barbershop.api.inventorymovement.repository.InventoryMovementRepository;
import dev.barbershop.api.product.dto.ProductCreateDTO;
import dev.barbershop.api.product.dto.ProductSearchDTO;
import dev.barbershop.api.product.entity.Product;
import dev.barbershop.api.product.repository.ProductRepository;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final InventoryMovementRepository movementRepository;
    private final UserRepository userRepository;

    public List<Product> search(ProductSearchDTO filters) {
        Product probe = Product.builder()
                .name(filters.getName())
                .description(filters.getDescription())
                .unitMeasurement(filters.getUnitMeasurement())
                .active(filters.getActive())
                .build();

        ExampleMatcher matcher = ExampleMatcher.matchingAll()
                .withIgnoreNullValues()
                .withMatcher("name",
                        ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withMatcher("description",
                        ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase());

        return repository.findAll(Example.of(probe, matcher));
    }

    @Transactional
    public Product create(ProductCreateDTO dto) {
        CustomUserDetails principal = (CustomUserDetails)
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Usuário não encontrado"));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .minimumQuantity(dto.getMinimumQuantity())
                .currentQuantity(dto.getInitialQuantity())
                .unitMeasurement(dto.getUnitMeasurement())
                .priceCost(dto.getPriceCost())
                .active(true)
                .build();

        product = repository.save(product);

        if (dto.getInitialQuantity().compareTo(BigDecimal.ZERO) > 0) {
            InventoryMovement movement = InventoryMovement.builder()
                    .user(user)
                    .product(product)
                    .quantity(dto.getInitialQuantity())
                    .type(InventoryMovementType.ENTRY)
                    .build();

            movementRepository.save(movement);
        }

        return product;
    }

    
    @Transactional
    public Product update(
            java.util.UUID id,
            dev.barbershop.api.product.dto.ProductUpdateDTO dto) {

        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Produto não encontrado"));

        Product updatedProduct = Product.builder()
                .id(product.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .minimumQuantity(dto.getMinimumQuantity())
                .currentQuantity(product.getCurrentQuantity())
                .unitMeasurement(dto.getUnitMeasurement())
                .priceCost(dto.getPriceCost())
                .active(dto.getActive())
                .build();

        return repository.save(updatedProduct);
    }

    
    @Transactional
    public void delete(java.util.UUID id) {
        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Produto não encontrado"));

        Product deactivatedProduct = Product.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .minimumQuantity(product.getMinimumQuantity())
                .currentQuantity(product.getCurrentQuantity())
                .unitMeasurement(product.getUnitMeasurement())
                .priceCost(product.getPriceCost())
                .active(false)
                .build();

        repository.save(deactivatedProduct);
    }
}
