package dev.barbershop.api.auth.authorization.repository;

import dev.barbershop.api.auth.authorization.entity.Permission;
import dev.barbershop.api.auth.authorization.enums.PermissionName;
import dev.barbershop.api.auth.authorization.dto.PermissionSummaryDTO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    List<PermissionSummaryDTO> findAllProjectedBy();

    Optional<PermissionSummaryDTO> findProjectedById(UUID id);

    boolean existsByName(PermissionName name);
}
