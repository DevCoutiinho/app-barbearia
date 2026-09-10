package dev.barbershop.api.auth.authorization.repository;

import dev.barbershop.api.auth.authorization.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
}
