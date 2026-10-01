package dev.barbershop.api.user.entity;


import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.common.auditing.Auditable;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.notification.entity.Notification;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true,
        callSuper = false
)
@ToString(exclude = {
        "inventoryMovements",
        "roles",
        "notifications",
        "barberProfile",
        "clientProfile"
})
@Table(name = "users")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    private String avatar;

    @Column(unique = true, nullable = false)
    private String email;

    @Setter
    @Column(nullable = false)
    private @NonNull String password;

    @Column(length = 11)
    private String telephone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    List<InventoryMovement> inventoryMovements = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false)
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Barber barberProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Client clientProfile;

    public void addRole(Role role) {
        Objects.requireNonNull(role);
        roles.add(role);
        role.addUser(this);
    }

    public void addClient(Client client) {
        this.clientProfile = Objects.requireNonNull(client);
    }

    public void addBarber(Barber barber){
        this.barberProfile = Objects.requireNonNull(barber);
    }
}
