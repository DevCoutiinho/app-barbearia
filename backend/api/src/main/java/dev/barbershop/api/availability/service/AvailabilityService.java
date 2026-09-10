package dev.barbershop.api.availability.service;

import dev.barbershop.api.availability.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository repository;
}
