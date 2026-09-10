package dev.barbershop.api.barberservice.entity;

import dev.barbershop.api.common.auditing.Creatable;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
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
@ToString
@Table(name = "service_categories")
public class ServiceCategory extends Creatable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String description;

    @JoinColumn(nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    List<BarberService> services = new ArrayList<>();
}
