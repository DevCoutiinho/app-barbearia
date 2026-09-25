package dev.barbershop.api.user.controller;

import dev.barbershop.api.user.dto.UserAdmDTO;
import dev.barbershop.api.user.dto.UserRoleUpdateDTO;
import dev.barbershop.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {

    private final UserService service;

    @GetMapping
    public List<UserAdmDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserAdmDTO findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/search/name")
    public List<UserAdmDTO> findByName(@RequestParam String name) {
        return service.findByName(name);
    }

    @GetMapping("/search/email")
    public List<UserAdmDTO> findByEmail(@RequestParam String email) {
        return service.findByEmail(email);
    }

    @GetMapping("/search/telephone")
    public List<UserAdmDTO> findByTelephone(@RequestParam String telephone) {
        return service.findByTelephone(telephone);
    }

    @PutMapping("/{id}/roles")
    public void updateRoles(
        @PathVariable UUID id,
        @RequestBody UserRoleUpdateDTO dto
    ) {
        service.updateRoles(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        service.delete(id, authentication);
    }
}