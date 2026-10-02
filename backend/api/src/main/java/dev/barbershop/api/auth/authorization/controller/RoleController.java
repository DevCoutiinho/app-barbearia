package dev.barbershop.api.auth.authorization.controller;

import dev.barbershop.api.auth.authorization.dto.RoleCreateDTO;
import dev.barbershop.api.auth.authorization.dto.RoleDTO;
import dev.barbershop.api.auth.authorization.service.RoleService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleDTO>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<RoleDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description
    ) {
        return ResponseEntity.ok(roleService.search(name, description));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RoleDTO> create(@RequestBody RoleCreateDTO dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roleService.create(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<RoleDTO> addPermission(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId
    ) {
        return ResponseEntity.ok(
                roleService.addPermission(roleId, permissionId)
        );
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<RoleDTO> removePermission(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId
    ) {
        return ResponseEntity.ok(
                roleService.removePermission(roleId, permissionId)
        );
    }
}
