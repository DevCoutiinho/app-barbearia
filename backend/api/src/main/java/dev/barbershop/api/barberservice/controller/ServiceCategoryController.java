package dev.barbershop.api.barberservice.controller;

import dev.barbershop.api.barberservice.controller.documentation.ServiceCategoryDoc;
import dev.barbershop.api.barberservice.dto.servicecategory.ServiceCategoryCreateDTO;
import dev.barbershop.api.barberservice.projection.ServiceCategorySummaryProjection;
import dev.barbershop.api.barberservice.service.ServiceCategoryService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ServiceCategoryController implements ServiceCategoryDoc {

    private final ServiceCategoryService service;

    @Override
    public ResponseEntity<ApiResponseDTO<ServiceCategorySummaryProjection>> findAll() {
        List<ServiceCategorySummaryProjection> list = service.findAllSummary();

        ApiResponseDTO<ServiceCategorySummaryProjection> responseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Categorias retornadas com sucesso",
                list
        );

        return ResponseEntity.ok(responseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> create(@Valid @RequestBody ServiceCategoryCreateDTO dto) {
        service.create(dto);

        ApiResponseDTO<Void> responseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Categoria criado com sucesso",
                null
        );

        return ResponseEntity.ok(responseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> update(@PathVariable UUID id, @Valid @RequestBody ServiceCategoryCreateDTO dto) {
        service.update(id, dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(HttpStatus.NO_CONTENT.value(),
                "Categoria alterada com sucesso",
                null);

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable UUID id) {

        service.delete(id);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Categoria excluida com sucesso",
                null);

        return ResponseEntity.ok(apiResponseDTO);
    }
}
