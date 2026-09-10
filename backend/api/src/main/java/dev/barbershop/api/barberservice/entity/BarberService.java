package dev.barbershop.api.barberservice.entity;

import dev.barbershop.api.barberservice.enums.BarberServiceTechnique;
import dev.barbershop.api.common.auditing.Auditable;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
@ToString(exclude = {"barber", "category", "schedulings", "images"})
@Table(name = "services")
public class BarberService extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private User barber;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BarberServiceTechnique technique;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "duration_time", nullable = false)
    private Integer durationTime;

    @OneToMany(mappedBy = "service")
    @Builder.Default
    List<Scheduling> schedulings = new ArrayList<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ServiceImage> images = new ArrayList<>();
}
