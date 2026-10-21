package dev.barbershop.api.barberservice.entity;

import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.barberservice.enums.BarberServiceTechnique;
import dev.barbershop.api.common.auditing.Auditable;
import dev.barbershop.api.common.exception.ResourceConflictException;
import dev.barbershop.api.scheduling.entity.Scheduling;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
@ToString(exclude = {"barber", "category", "schedulings", "images"})
@Table(name = "services")
public class BarberService extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

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

    public void changeName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public void changeTechnique(BarberServiceTechnique technique){
        Objects.requireNonNull(technique);

        if(technique.name().length() > 50){
            throw new ResourceConflictException("o nome da técnica não pode ser maior que 50 caracteres");
        }

        this.technique = technique;
    }

    public void changePrice(BigDecimal price) {
        Objects.requireNonNull(price);

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResourceConflictException("O preço não pode ser menor que 0");
        }

        this.price = price;
    }

    public void changeActive(Boolean active) {
        this.active = Objects.requireNonNull(active);
    }

    public void changeDurationTime(Integer durationTime) {
        this.durationTime = Objects.requireNonNull(durationTime);
    }


    public void addBarber(Barber barber) {
        this.barber = Objects.requireNonNull(barber);
        barber.addService(this);
    }

    public void addCategory(ServiceCategory category) {
        this.category = Objects.requireNonNull(category, "A categoria é obrigatória");
    }

    public void addImage(ServiceImage image) {
        Objects.requireNonNull(image, "A imagem não pode ser nula");
        image.assignToService(this);
        this.images.add(image);
    }
}
