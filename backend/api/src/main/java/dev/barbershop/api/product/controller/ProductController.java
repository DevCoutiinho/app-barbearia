package dev.barbershop.api.product.controller;

import dev.barbershop.api.product.dto.ProductCreateDTO;
import dev.barbershop.api.product.dto.ProductSearchDTO;
import dev.barbershop.api.product.dto.ProductUpdateDTO;
import dev.barbershop.api.product.entity.Product;
import dev.barbershop.api.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/stock/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public List<Product> search(ProductSearchDTO filters) {
        return service.search(filters);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public Product create(@Valid @RequestBody ProductCreateDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public Product update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_DELETE')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
