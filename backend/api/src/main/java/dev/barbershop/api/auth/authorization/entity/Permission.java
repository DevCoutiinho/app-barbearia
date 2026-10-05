package dev.barbershop.api.auth.authorization.entity;

import dev.barbershop.api.auth.authorization.enums.PermissionName;
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
@ToString(exclude = "roles")
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 50)
    private PermissionName name;

    private String description;

    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {
        Objects.requireNonNull(role);

        roles.add(role);
    }

    public void removeRole(Role role) {
        Objects.requireNonNull(role);

        roles.remove(role);
    }
}
