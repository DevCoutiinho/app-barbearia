package dev.barbershop.api.product.service;

import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.inventorymovement.enums.InventoryMovementType;
import dev.barbershop.api.inventorymovement.repository.InventoryMovementRepository;
import dev.barbershop.api.product.dto.ProductCreateDTO;
import dev.barbershop.api.product.dto.ProductFilterDTO;
import dev.barbershop.api.product.dto.ProductUpdateDTO;
import dev.barbershop.api.product.entity.Product;
import dev.barbershop.api.product.mapper.ProductMapper;
import dev.barbershop.api.product.projection.ProductProjection;
import dev.barbershop.api.product.repository.ProductRepository;
import dev.barbershop.api.product.specification.ProductSpecification;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final InventoryMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final ProductMapper mapper;

    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public Page<ProductProjection> findAll(ProductFilterDTO dto, Pageable pageable) {

        Specification<Product> spec = ProductSpecification.findByFilter(dto);

        return repository.findBy(spec,
                q -> q.as(ProductProjection.class)
                        .page(pageable));
    }

    @Transactional
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public void create(UUID userId, ProductCreateDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Usuário não encontrado"));

        Product product = mapper.toEntity(dto);
        product = repository.save(product);

        if (dto.initialQuantity().compareTo(BigDecimal.ZERO) > 0) {
            InventoryMovement movement = InventoryMovement.builder()
                    .user(user)
                    .product(product)
                    .quantity(dto.initialQuantity())
                    .type(InventoryMovementType.ENTRY)
                    .build();

            movementRepository.save(movement);
        }
    }


    @Transactional
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public void update(UUID id, ProductUpdateDTO dto) {
        Product product = getProductId(id);

        product.changeName(dto.name());
        product.changeMinimumQuantity(dto.minimumQuantity());
        product.changeUnitMeasurement(dto.unitMeasurement());
        product.changePriceCost(dto.priceCost());

        repository.save(product);

    }

    @Transactional
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public void activateProduct(UUID id) {
        Product product = getProductId(id);
        product.activate();

        repository.save(product);
    }

    @Transactional
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public void deactivateProduct(UUID id) {
        Product product = getProductId(id);
        product.deactivate();

        repository.save(product);
    }

    public Product getProductId(UUID id) throws ResourceNotFoundException {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
    }


}
