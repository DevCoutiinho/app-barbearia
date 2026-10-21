package dev.barbershop.api.barberservice.entity;

import dev.barbershop.api.common.auditing.Creatable;
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
@ToString(exclude = "services")
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
    private boolean active = true;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    List<BarberService> services = new ArrayList<>();

    public void changeName(String name){
        this.name = Objects.requireNonNull(name);
    }

    public void changeDescription(String description){
        this.description = description;
    }

    public void changeActive(boolean active){
        this.active = active;
    }

    public void addService(BarberService service){
        Objects.requireNonNull(service);
        this.services.add(service);
    }
}
