package dev.barbershop.api.auth.authorization.service;

import dev.barbershop.api.auth.authorization.dto.RoleCreateDTO;
import dev.barbershop.api.auth.authorization.dto.RoleResponseDTO;
import dev.barbershop.api.auth.authorization.dto.RoleUpdateDTO;
import dev.barbershop.api.auth.authorization.entity.Permission;
import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.enums.RoleName;
import dev.barbershop.api.auth.authorization.mapper.RoleMapper;
import dev.barbershop.api.auth.authorization.projection.RoleSummaryProjection;
import dev.barbershop.api.auth.authorization.repository.PermissionRepository;
import dev.barbershop.api.auth.authorization.repository.RoleRepository;
import dev.barbershop.api.common.exception.ResourceAlreadyExistsException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {
    private final RoleRepository repository;
    private final RoleMapper mapper;
    private final PermissionRepository permissionRepository;

    @PreAuthorize("hasAuthority('ROLE_READ')")
    public List<RoleResponseDTO> findAll() {
        List<Role> roles = repository.findAllRolesWithPermissions();

        return roles.stream()
                .map(mapper::toRoleResponseDTO)
                .toList();
    }

    @PreAuthorize("hasAuthority('ROLE_READ')")
    public List<RoleSummaryProjection> findAllRolesSummaries() {
        return repository.findAllRolesSummaries();
    }

    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    @Transactional
    public void create(RoleCreateDTO dto) {

        if (existingRole(dto.name())) {
            throw new ResourceAlreadyExistsException("Já existe uma role com esse nome cadastrado");
        }

        List<Permission> permissions = findPermissions(dto.permissionIds());

        if (permissions.size() != dto.permissionIds().size()) {
            throw new ResourceNotFoundException("Uma ou mais permissões não existem");
        }

        Role role = mapper.toEntity(dto);

        permissions.forEach(role::addPermission);

        repository.save(role);

    }

    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    @Transactional
    public void update(UUID id, RoleUpdateDTO dto) {

        Role role = findById(id);

        role.changeDescription(dto.description());

        if (dto.permissionIds() != null) {

            if (dto.permissionIds().isEmpty()) {
                throw new IllegalArgumentException("A role deve ter pelo menos uma permissão");
            }

            Set<UUID> targetIds = dto.permissionIds();

            List<Permission> targetPermissions = findPermissions(targetIds);

            if (targetPermissions.size() != targetIds.size()) {
                throw new ResourceNotFoundException("Uma ou mais permissões não existem");
            }

            List<Permission> toRemove = role.getPermissions().stream()
                    .filter(p -> !targetIds.contains(p.getId()))
                    .toList();

            toRemove.forEach(role::removePermission);

            Set<UUID> currentIds = role.getPermissions().stream()
                    .map(Permission::getId)
                    .collect(Collectors.toSet());

            List<Permission> toAdd = targetPermissions.stream()
                    .filter(p -> !currentIds.contains(p.getId()))
                    .toList();

            toAdd.forEach(role::addPermission);
        }

        repository.save(role);
    }

    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    @Transactional
    public void delete(UUID id) throws ResourceNotFoundException {
        Role role = findById(id);

        repository.delete(role);
    }

    private List<Permission> findPermissions(Set<UUID> ids) {
        return permissionRepository.findAllById(ids);
    }

    private boolean existingRole(RoleName name) {
        return repository.existsByName(name);
    }

    private Role findById(UUID id) throws ResourceNotFoundException {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma role encontrada com esse ID"));
    }
}
