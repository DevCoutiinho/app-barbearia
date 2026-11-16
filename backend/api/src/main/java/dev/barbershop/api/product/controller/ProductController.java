package dev.barbershop.api.product.controller;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.product.controller.documentation.ProductControllerDoc;
import dev.barbershop.api.product.dto.ProductCreateDTO;
import dev.barbershop.api.product.dto.ProductFilterDTO;
import dev.barbershop.api.product.dto.ProductUpdateDTO;
import dev.barbershop.api.product.projection.ProductProjection;
import dev.barbershop.api.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProductController implements ProductControllerDoc {

    private final ProductService service;


    @Override
    public ResponseEntity<ApiResponseDTO<Page<ProductProjection>>> findAll(
            @ParameterObject
            @ModelAttribute
            ProductFilterDTO dto,

            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = {"name"},
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        Page<ProductProjection> products = service.findAll(dto, pageable);

        ApiResponseDTO<Page<ProductProjection>> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Produtos retornados com sucesso",
                List.of(products)
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> create(@AuthenticationPrincipal CustomUserDetails customUser, @Valid @RequestBody ProductCreateDTO dto) {
        service.create(customUser.getId(), dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Produto adicionado com sucesso",
                null
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateDTO dto) {
        service.update(id,dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.NO_CONTENT.value(),
                "Produto desativado com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);

    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> deactivateProduct(@PathVariable UUID id) {
        service.deactivateProduct(id);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.NO_CONTENT.value(),
                "Produto desativado com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);

    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> activateProduct(@PathVariable UUID id) {
        service.activateProduct(id);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.NO_CONTENT.value(),
                "Produto ativado com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);
    }
}
