package dev.barbershop.api.auth.authentication.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import dev.barbershop.api.auth.authorization.enums.RoleName;
import dev.barbershop.api.common.security.AuthorityMapper;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.config.security.JwtProperties;
import dev.barbershop.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProperties jwtProperties;
    private final AuthorityMapper authorityMapper;

    public String generateToken(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        return generateToken(user);
    }

    public String generateToken(User user) {

        Set<GrantedAuthority> authorities = authorityMapper.mapToAuthorities(user);
        Set<RoleName> roles = authorityMapper.mapToRoleName(user);

        CustomUserDetails userDetails =
                new CustomUserDetails(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPassword(),
                        roles,
                        authorities
                );

        return generateToken(userDetails);

    }

    private String generateToken(CustomUserDetails user) {

        try {
            Algorithm algorithm = Algorithm
                    .HMAC256(jwtProperties.getSecret());

            Instant expirationDate = Instant.now()
                    .plusSeconds(jwtProperties.getLocalExpiration());

            List<String> permissions = user.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            List<String> roles = user.getRoles()
                    .stream()
                    .map(RoleName::name)
                    .toList();

            return JWT.create()
                    .withIssuer(jwtProperties.getLocalIssuer())
                    .withSubject(user.getId().toString())
                    .withClaim("name", user.getName())
                    .withClaim("email", user.getEmail())
                    .withClaim("roles", roles)
                    .withClaim("permissions", permissions)
                    .withExpiresAt(expirationDate)
                    .sign(algorithm);

        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar o token JWT ", exception);
        }
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}
