package dev.barbershop.api.auth.authentication.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleTokenService {

    private final JwtDecoder googleDecoder;

    public Jwt validate(String idToken) {
        return googleDecoder.decode(idToken);
    }
}
