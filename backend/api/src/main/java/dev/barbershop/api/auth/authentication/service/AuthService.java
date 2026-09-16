package dev.barbershop.api.auth.authentication.service;

import dev.barbershop.api.auth.authentication.dto.GoogleLoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginResponseDTO;
import dev.barbershop.api.auth.authentication.mapper.AuthMapper;
import dev.barbershop.api.auth.authentication.dto.UserCreateDTO;
import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.enums.RoleName;
import dev.barbershop.api.auth.authorization.repository.RoleRepository;
import dev.barbershop.api.common.exception.ResourceAlreadyExistsException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.user.dto.UserCreatedDTO;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper mapper;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RoleRepository roleRepository;
    private final GoogleTokenService googleTokenService;

    @Transactional
    public UserCreatedDTO register(UserCreateDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new ResourceAlreadyExistsException("Email já cadastrado");
        }

        Role defaultRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Nennhuma role encontra com o nome USER"));

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(Objects.requireNonNull(passwordEncoder.encode(dto.password())))
                .build();

        user.addRole(defaultRole);

        return mapper.toDTO(userRepository.save(user));
    }

    public LoginResponseDTO login(LoginDTO dto) {
        try {
            var authenticateToken = new UsernamePasswordAuthenticationToken(
                    dto.email(),
                    dto.password()
            );

            Authentication authentication = authenticationManager.authenticate(authenticateToken);

            String jwt = tokenService.generateToken(authentication);

            return new LoginResponseDTO(jwt);
        } catch (Exception e) {
            log.error("Erro na autenticação para email={}: {}", dto.email(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public LoginResponseDTO loginWithGoogle(GoogleLoginDTO dto) {


        Jwt googleJwt = googleTokenService.validate(dto.idToken());
        String email = googleJwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Token Google não contém email"
            );
        }

        User user = userRepository.findByEmailWitchRoles(email)
                .orElseGet(() -> createGoogleUser(googleJwt, email));

        String jwt = tokenService.generateToken(user);

        return new LoginResponseDTO(jwt);
    }

    private User createGoogleUser(Jwt googleJwt, String email) {
        String name = googleJwt.getClaimAsString("name");
        String picture = googleJwt.getClaimAsString("picture");

        Role defaultRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Nennhuma role encontra com o nome USER"));

        User user = User.builder()
                .name(name != null ? name : "Usuário Google")
                .email(email)
                .password(
                        Objects.requireNonNull(
                                passwordEncoder.encode(UUID.randomUUID().toString())
                        ))
                .avatar(picture)
                .build();

        user.addRole(defaultRole);

        return userRepository.save(user);
    }
}
