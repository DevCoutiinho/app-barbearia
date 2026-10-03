package dev.barbershop.api.user.admin.service;

import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.repository.RoleRepository;
import dev.barbershop.api.common.exception.ResourceAlreadyExistsException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.user.admin.dto.UserAdminResponseDTO;
import dev.barbershop.api.user.admin.dto.UserUpdateRolesDTO;
import dev.barbershop.api.user.admin.specification.UserSpecification;
import dev.barbershop.api.user.mapper.UserMapper;
import dev.barbershop.api.user.admin.dto.UserFilterDTO;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
public class UserService {
    private final UserMapper mapper;
    private final UserRepository repository;
    private final RoleRepository roleRepository;

    @PreAuthorize("hasAuthority('USER_READ')")
    public Page<UserAdminResponseDTO> findAll(UserFilterDTO dto, Pageable pageable){

        Specification<User> spec = UserSpecification.findByFilter(dto);

        Page<User> users = repository.findAll( spec, pageable);

        return users.map(mapper::toAdminResponseDTO);
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Transactional
    public void updateRoles(UUID id, UserUpdateRolesDTO dto) {

        User user = findById(id);

        if (dto.roleIds() != null) {
            Set<UUID> targetIds = dto.roleIds();

            List<Role> toRemove = user.getRoles().stream()
                    .filter(role -> !targetIds.contains(role.getId()))
                    .toList();

            toRemove.forEach(user::removeRole);

            if (!targetIds.isEmpty()) {
                List<Role> targetRoles = roleRepository.findAllById(targetIds);

                Set<UUID> currentIds = user.getRoles().stream()
                        .map(Role::getId)
                        .collect(Collectors.toSet());

                List<Role> toAdd = targetRoles.stream()
                        .filter(role -> !currentIds.contains(role.getId()))
                        .toList();

                toAdd.forEach(user::addRole);
            }
        }
    }

    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Transactional
    public void delete(UUID userId, UUID ownerId) {

        User user = findById(userId);

        if(user.getId().equals(ownerId)){
            throw new IllegalStateException(
                    "O administrador não pode excluir a própria conta"
            );
        }

        repository.delete(user);
    }

    private User findById(UUID id) throws ResourceAlreadyExistsException {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum usuário encontrado com esse ID"));
    }
}
