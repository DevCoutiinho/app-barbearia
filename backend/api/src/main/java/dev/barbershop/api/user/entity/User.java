package dev.barbershop.api.user.entity;


import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.availability.entity.Availability;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.common.auditing.Auditable;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.notification.entity.Notification;
import dev.barbershop.api.scheduling.entity.Scheduling;
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
        "availabilities",
        "services",
        "barberSchedulings",
        "clientSchedulings",
        "notifications"
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

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 11)
    private String telephone;

    @Column(nullable = false)
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

    @OneToMany(mappedBy = "barber", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Availability> availabilities = new ArrayList<>();

    @OneToMany(mappedBy = "barber")
    @Builder.Default
    List<BarberService> services = new ArrayList<>();

    @OneToMany(mappedBy = "barber")
    @Builder.Default
    private List<Scheduling> barberSchedulings = new ArrayList<>();

    @OneToMany(mappedBy = "client")
    @Builder.Default
    private List<Scheduling> clientSchedulings = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();
}
