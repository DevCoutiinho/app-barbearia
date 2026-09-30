package dev.barbershop.api.user.service;

import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.mapper.UserMapper;
import dev.barbershop.api.user.repository.UserRepository;
import dev.barbershop.api.user.dto.UserAdmDTO;
import dev.barbershop.api.user.dto.UserRoleUpdateDTO;
import dev.barbershop.api.auth.authorization.repository.RoleRepository;
import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public List<UserAdmDTO> findAll() {
        return repository.findAll()
            .stream()
            .map(mapper::toAdmDTO)
            .toList();
    }

    public UserAdmDTO findById(UUID id) {
        User user = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return mapper.toAdmDTO(user);
    }

    public List<UserAdmDTO> findByName(String name) {
        User probe = User.builder()
            .name(name)
            .build();

        Example<User> example = Example.of(probe, defaultMatcher());

        return repository.findAll(example)
            .stream()
            .map(mapper::toAdmDTO)
            .toList();
    }

    public List<UserAdmDTO> findByEmail(String email) {
        User probe = User.builder()
            .email(email)
            .build();

        Example<User> example = Example.of(probe, defaultMatcher());

        return repository.findAll(example)
            .stream()
            .map(mapper::toAdmDTO)
            .toList();
    }

    public List<UserAdmDTO> findByTelephone(String telephone) {
        User probe = User.builder()
            .telephone(telephone)
            .build();

        Example<User> example = Example.of(probe, defaultMatcher());

        return repository.findAll(example)
            .stream()
            .map(mapper::toAdmDTO)
            .toList();
    }

    private ExampleMatcher defaultMatcher() {
        return ExampleMatcher.matching()
            .withIgnoreCase()
            .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
    }

    @Transactional
    public void updateRoles(UUID id, UserRoleUpdateDTO dto) {
        User user = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Set<Role> roles = dto.roles().stream()
            .map(roleName -> roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada")))
            .collect(Collectors.toSet());

        user.replaceRoles(roles);
    }

    @Transactional
    public void delete(UUID id, Authentication authentication) {
        User user = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        CustomUserDetails currentUser =
            (CustomUserDetails) authentication.getPrincipal();

        if (user.getId().equals(currentUser.getId())) {
            throw new IllegalStateException(
                "O administrador não pode excluir a própria conta"
            );
        }

        repository.delete(user);
    }
}
