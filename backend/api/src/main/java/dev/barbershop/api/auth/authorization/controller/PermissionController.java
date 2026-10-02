package dev.barbershop.api.auth.authorization.controller;

import dev.barbershop.api.auth.authorization.dto.PermissionCreateDTO;
import dev.barbershop.api.auth.authorization.dto.PermissionDTO;
import dev.barbershop.api.auth.authorization.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class PermissionController {

    private final PermissionService service;

    @GetMapping
    public List<PermissionDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PermissionDTO findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/search")
    public List<PermissionDTO> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String description
    ) {
        return service.search(name, description);
    }

    @PostMapping
    public PermissionDTO create(@RequestBody PermissionCreateDTO dto) {
        return service.create(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
