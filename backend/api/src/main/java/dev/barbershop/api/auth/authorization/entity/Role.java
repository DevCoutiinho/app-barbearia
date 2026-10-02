package dev.barbershop.api.auth.authorization.entity;

import dev.barbershop.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"users", "permissions"})
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    private String description;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @ManyToMany()
    @JoinTable(
            name = "roles_permissions",
            joinColumns = @JoinColumn(name = "role_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "permission_id", nullable = false)
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    public void addUser(User user) {
        Objects.requireNonNull(user);
        users.add(user);
    }

    public void addPermission(Permission permission) {
        Objects.requireNonNull(permission);
        permissions.add(permission);
        permission.getRoles().add(this);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
        permission.getRoles().remove(this);
    }

    public void removeAllPermissions() {
        for (Permission permission : new HashSet<>(permissions)) {
            removePermission(permission);
        }
    }
}
