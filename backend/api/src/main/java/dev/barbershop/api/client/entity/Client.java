package dev.barbershop.api.client.entity;

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
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true,
        callSuper = false
)
@ToString(exclude = {
        "user",
        "schedulings"
})
@Table(name = "clients")
public class Client extends Creatable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "client")
    @Builder.Default
    private List<Scheduling> schedulings = new ArrayList<>();

    public void addUser(User user) {
        Objects.requireNonNull(user);
        this.user = user;
        user.addClient(this);
    }

}
