package dev.barbershop.api.auth.authorization.service;

import dev.barbershop.api.auth.authorization.dto.PermissionCreateDTO;
import dev.barbershop.api.auth.authorization.entity.Permission;
import dev.barbershop.api.auth.authorization.enums.PermissionName;
import dev.barbershop.api.auth.authorization.mapper.PermissionMapper;
import dev.barbershop.api.auth.authorization.dto.PermissionSummaryDTO;
import dev.barbershop.api.auth.authorization.repository.PermissionRepository;
import dev.barbershop.api.common.exception.ResourceAlreadyExistsException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository repository;
    private final PermissionMapper mapper;

    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    public List<PermissionSummaryDTO> findAllSummaries() {
        return repository.findAllProjectedBy();
    }

    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    public PermissionSummaryDTO findById(UUID id) {
        return repository.findProjectedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma permissão com esse ID foi encontrada"));
    }

    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    @Transactional
    public void create(PermissionCreateDTO dto) {

        if (existingPermission(dto.name())) {
            throw new ResourceAlreadyExistsException("Já existe uma permissão com esse nome");
        }

        Permission permission = mapper.toEntity(dto);

        repository.save(permission);
    }

    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    @Transactional
    public void delete(UUID id) {
        Permission permission = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma permissão com esse ID foi encontrada"));

        repository.delete(permission);
    }

    private boolean existingPermission(PermissionName name) {
        return repository.existsByName(name);
    }

}
