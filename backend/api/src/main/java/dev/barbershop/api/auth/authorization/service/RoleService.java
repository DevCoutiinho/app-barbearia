package dev.barbershop.api.auth.authorization.service;

import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.auth.authorization.dto.RoleCreateDTO;
import dev.barbershop.api.auth.authorization.dto.RoleDTO;
import dev.barbershop.api.auth.authorization.dto.PermissionDTO;
import dev.barbershop.api.auth.authorization.entity.Permission;
import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.repository.PermissionRepository;
import dev.barbershop.api.auth.authorization.repository.RoleRepository;
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
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleDTO> findAll() {
        return roleRepository.findAll()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    public RoleDTO findById(UUID id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));

        return toDTO(role);
    }

    public List<RoleDTO> search(String name, String description) {
        Role probe = Role.builder()
            .name(name)
            .description(description)
            .build();

        ExampleMatcher matcher = ExampleMatcher.matching()
            .withIgnoreCase()
            .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        Example<Role> example = Example.of(probe, matcher);

        return roleRepository.findAll(example)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public RoleDTO create(RoleCreateDTO dto) {
        if (roleRepository.existsByName(dto.name())) {
            throw new ResourceAlreadyExistsException("Role já existe");
        }

        Role role = Role.builder()
            .name(dto.name())
            .description(dto.description())
            .build();

        return toDTO(roleRepository.save(role));
    }

    @Transactional
    public void delete(UUID id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));

        for (User user : new HashSet<>(role.getUsers())) {
            user.removeRole(role);
        }

        role.removeAllPermissions();

        roleRepository.delete(role);
    }

    @Transactional
    public RoleDTO addPermission(UUID roleId, UUID permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));

        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada"));

        role.addPermission(permission);

        return toDTO(role);
    }

    @Transactional
    public RoleDTO removePermission(UUID roleId, UUID permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));

        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada"));

        role.removePermission(permission);

        return toDTO(role);
    }

    private RoleDTO toDTO(Role role) {
        return new RoleDTO(
            role.getId(),
            role.getName(),
            role.getDescription(),
            role.getPermissions()
                .stream()
                .map(permission -> new PermissionDTO(
                    permission.getId(),
                    permission.getName(),
                    permission.getDescription()
                ))
                .collect(java.util.stream.Collectors.toSet())
        );
    }
}
