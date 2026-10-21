package dev.barbershop.api.barberservice.service;

import dev.barbershop.api.barberservice.dto.servicecategory.ServiceCategoryCreateDTO;
import dev.barbershop.api.barberservice.entity.ServiceCategory;
import dev.barbershop.api.barberservice.mapper.ServiceCategoryMapper;
import dev.barbershop.api.barberservice.projection.ServiceCategorySummaryProjection;
import dev.barbershop.api.barberservice.repository.BarberServiceRepository;
import dev.barbershop.api.barberservice.repository.ServiceCategoryRepository;
import dev.barbershop.api.common.exception.ResourceAlreadyExistsException;
import dev.barbershop.api.common.exception.ResourceConflictException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCategoryService {
    private final ServiceCategoryRepository repository;
    private final ServiceCategoryMapper mapper;
    private final BarberServiceRepository serviceRepository;

    @PreAuthorize("hasRole('BARBER')")
    public List<ServiceCategorySummaryProjection> findAllSummary() {
        return repository.findAllProjectedBy();
    }

    @Transactional
    @PreAuthorize("hasRole('BARBER')")
    public void create(ServiceCategoryCreateDTO dto) {

        if (existsCategory(dto.name())) {
            throw new ResourceAlreadyExistsException("Já existe uma categoria com esse nome");
        }

        ServiceCategory serviceCategory = mapper.toEntity(dto);

        repository.save(serviceCategory);
    }

    @Transactional
    @PreAuthorize("hasRole('BARBER')")
    public void delete(UUID id) {
        ServiceCategory category = getById(id);

        if (serviceRepository.existsByCategory_Id(id)) {
            throw new ResourceConflictException("Não é possivel excluir categorias de serviços que possui serviços associados");
        }

        repository.delete(category);

    }

    @Transactional
    @PreAuthorize("hasRole('BARBER')")
    public void update(UUID id, ServiceCategoryCreateDTO dto) {

        ServiceCategory category = getById(id);

        if (existsCategory(dto.name()) && category.getId() != id) {
            throw new ResourceAlreadyExistsException("Já existe uma categoria com esse nome");
        }

        category.changeName(dto.name());
        category.changeDescription(dto.description());
        category.changeActive(dto.active());

        repository.save(category);
    }

    private boolean existsCategory(String name) {
        return repository.existsByName(name);
    }

    private ServiceCategory getById(UUID id) throws ResourceNotFoundException {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma categoria de serviço encontrada com esse ID"));
    }

}
