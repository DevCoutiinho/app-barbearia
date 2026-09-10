package dev.barbershop.api.auth.authorization.repository;

import dev.barbershop.api.auth.authorization.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
}
