package dev.barbershop.api.barberservice.service;

import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.barber.repository.BarberRepository;
import dev.barbershop.api.barberservice.dto.BarberServiceCreateDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceFilterDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceResponseDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceUpdateDTO;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.barberservice.entity.ServiceCategory;
import dev.barbershop.api.barberservice.entity.ServiceImage;
import dev.barbershop.api.barberservice.mapper.BarberServiceMapper;
import dev.barbershop.api.barberservice.repository.BarberServiceRepository;
import dev.barbershop.api.barberservice.repository.ServiceCategoryRepository;
import dev.barbershop.api.barberservice.specification.BarberServiceSpecification;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BarberServiceApplicationService {

    private final BarberServiceRepository repository;
    private final ImageStorageService storageService;
    private final BarberServiceMapper mapper;
    private final BarberRepository barberRepository;
    private final ServiceCategoryRepository categoryRepository;


    @PreAuthorize("hasRole('BARBER')")
    @Transactional
    public Page<BarberServiceResponseDTO> findAll(UUID userId, BarberServiceFilterDTO dto, Pageable pageable) {
        Specification<BarberService> spec = BarberServiceSpecification.findByFilter(userId, dto);

        Page<BarberService> services = repository.findAll(spec, pageable);
        return services.map(mapper::toResponseDTO);
    }


    @PreAuthorize("hasRole('BARBER')")
    @Transactional
    public void create(UUID userId, MultipartFile image, BarberServiceCreateDTO dto) {

        Barber barber = findByBarberId(userId);

        ServiceCategory category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma categoria encontrada com esse ID"));

        BarberService service = mapper.toEntity(dto);
        service.addBarber(barber);
        service.addCategory(category);

        if (image != null && !image.isEmpty()) {
            String filename = storageService.store(image);
            ServiceImage serviceImage = createServiceImage(filename, dto.name(), true);

            service.addImage(serviceImage);
        }

        repository.save(service);
    }

    @PreAuthorize("hasRole('BARBER')")
    @Transactional
    public void update(UUID id, UUID userId, BarberServiceUpdateDTO dto) {

        Barber barber = findByBarberId(userId);

        BarberService service = repository.findByIdAndBarberId(id, barber.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum serviço com esse Id foi encontrado, associado a esse barbeiro com esse Id"));

        service.changeName(dto.name());
        service.changeTechnique(dto.technique());
        service.changePrice(dto.price());
        service.changeActive(dto.active());
        service.changeDurationTime(dto.durationTime());

        if (dto.categoryId() != null) {
            ServiceCategory category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nenhuma categoria encontrada com esse ID"));

            service.addCategory(category);

        }
        repository.save(service);
    }

    @PreAuthorize("hasRole('BARBER')")
    @Transactional
    public void delete(UUID id, UUID userId) {

        Barber barber = findByBarberId(userId);

        BarberService service = repository.findByIdAndBarberId(id, barber.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum serviço com esse Id foi encontrado, associado a esse barbeiro com esse Id"));

        if (service.getImages() != null && !service.getImages().isEmpty()) {
            service.getImages().forEach(image -> storageService.delete(image.getUrl()));
        }

        repository.delete(service);

    }

    private ServiceImage createServiceImage(String url, String altText, boolean isCover) {

        return ServiceImage.builder()
                .url(Objects.requireNonNull(url, "A URL da imagem não pode ser nula"))
                .altText(altText)
                .isCover(isCover)
                .build();
    }

    private Barber findByBarberId(UUID userId) {
        return barberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Não existe barbeiro associado ao usuário informado"));
    }
}
