package dev.barbershop.api.product.repository;

import dev.barbershop.api.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.util.UUID;

public interface ProductRepository extends
        JpaRepository<Product, UUID>,
        QueryByExampleExecutor<Product> {
}
