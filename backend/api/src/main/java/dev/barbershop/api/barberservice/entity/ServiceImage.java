package dev.barbershop.api.barberservice.entity;

import dev.barbershop.api.common.auditing.Creatable;
import jakarta.persistence.*;
import lombok.*;

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
@ToString(exclude = {"service"})
@Table(name = "service_images")
public class ServiceImage extends Creatable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private BarberService service;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "alt_text")
    private String altText;

    @Column(nullable = false)
    private Boolean isCover = Boolean.FALSE;
}
