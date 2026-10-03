package dev.barbershop.api.user.profile.controller;

import dev.barbershop.api.user.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;
}
