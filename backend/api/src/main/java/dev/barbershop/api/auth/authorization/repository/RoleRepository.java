package dev.barbershop.api.auth.authorization.repository;

import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.enums.RoleName;
import dev.barbershop.api.auth.authorization.projection.RoleSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    @Query(
            """
                SELECT r FROM Role r LEFT JOIN FETCH r.permissions
            """
    )
    List<Role> findAllRolesWithPermissions();

    @Query(
            """
                 SELECT new dev.barbershop.api.auth.authorization.projection.RoleSummaryProjection(r.id, r.name, r.description)
                  FROM Role r
            """
    )
    List<RoleSummaryProjection> findAllRolesSummaries();

    boolean existsByName(RoleName name);

    Optional<Role> findByName(RoleName name);
}
