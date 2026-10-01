package dev.barbershop.api.barber.entity;

import dev.barbershop.api.availability.entity.Availability;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.common.auditing.Creatable;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true,
        callSuper = false
)
@ToString(exclude = {
        "services",
        "schedulings",
        "availabilities",
        "user"
})
@Table(name = "barbers")
public class Barber extends Creatable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(length = 500)
    private String bio;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "barber")
    @Builder.Default
    private List<BarberService> services = new ArrayList<>();

    @OneToMany(mappedBy = "barber")
    @Builder.Default
    private List<Scheduling> schedulings = new ArrayList<>();

    @OneToMany(mappedBy = "barber")
    @Builder.Default
    private List<Availability> availabilities = new ArrayList<>();

    public void addUser(User user) {
        Objects.requireNonNull(user);
        this.user = user;
        user.addBarber(this);
    }
}
