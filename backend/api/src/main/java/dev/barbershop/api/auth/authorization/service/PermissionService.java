package dev.barbershop.api.auth.authorization.service;

import dev.barbershop.api.auth.authorization.dto.PermissionCreateDTO;
import dev.barbershop.api.auth.authorization.dto.PermissionDTO;
import dev.barbershop.api.auth.authorization.entity.Permission;
import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.repository.PermissionRepository;
import dev.barbershop.api.common.exception.ResourceAlreadyExistsException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository repository;

    public List<PermissionDTO> findAll() {
        return repository.findAll()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    public PermissionDTO findById(UUID id) {
        Permission permission = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada"));

        return toDTO(permission);
    }

    public List<PermissionDTO> search(String name, String description) {
        Permission probe = Permission.builder()
            .name(name)
            .description(description)
            .build();

        ExampleMatcher matcher = ExampleMatcher.matching()
            .withIgnoreCase()
            .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        Example<Permission> example = Example.of(probe, matcher);

        return repository.findAll(example)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public PermissionDTO create(PermissionCreateDTO dto) {
        if (repository.existsByName(dto.name())) {
            throw new ResourceAlreadyExistsException("Permissão já existe");
        }

        Permission permission = Permission.builder()
            .name(dto.name())
            .description(dto.description())
            .build();

        return toDTO(repository.save(permission));
    }

    @Transactional
    public void delete(UUID id) {
        Permission permission = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada"));

        for (Role role : new HashSet<>(permission.getRoles())) {
            role.removePermission(permission);
        }

        repository.delete(permission);
    }

    private PermissionDTO toDTO(Permission permission) {
        return new PermissionDTO(
            permission.getId(),
            permission.getName(),
            permission.getDescription()
        );
    }
}
