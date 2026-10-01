package dev.barbershop.api.auth.authentication.service;

import dev.barbershop.api.auth.authentication.dto.GoogleLoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginResponseDTO;
import dev.barbershop.api.auth.authentication.exception.InvalidTokenException;
import dev.barbershop.api.auth.authentication.exception.MissingRefreshTokenException;
import dev.barbershop.api.auth.authentication.mapper.AuthMapper;
import dev.barbershop.api.auth.authentication.dto.UserCreateDTO;
import dev.barbershop.api.auth.authentication.repository.RefreshTokenRepository;
import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.enums.RoleName;
import dev.barbershop.api.auth.authorization.repository.RoleRepository;
import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.common.exception.ResourceAlreadyExistsException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.security.CustomUserDetails;
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
import dev.barbershop.api.config.security.JwtProperties;
import dev.barbershop.api.auth.authentication.entity.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
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
    private final RefreshTokenRepository tokenRepository;
    private final JwtProperties jwtProperties;

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

        createClient(user);

        return mapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public LoginResponseDTO login(LoginDTO dto, HttpServletRequest request) {
        try {

            var authenticateToken = new UsernamePasswordAuthenticationToken(
                    dto.email(),
                    dto.password()
            );

            Authentication authentication = authenticationManager.authenticate(authenticateToken);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

            String jwt = tokenService.generateToken(authentication);
            String refreshToken = tokenService.generateRefreshToken();

            saveRefreshToken(user, request, refreshToken);

            return new LoginResponseDTO(jwt, refreshToken);
        } catch (Exception e) {
            log.error("Erro na autenticação para email={}: {}", dto.email(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public LoginResponseDTO loginWithGoogle(GoogleLoginDTO dto, HttpServletRequest request) {

        Jwt googleJwt = googleTokenService.validate(dto.idToken());
        String email = googleJwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            throw new InvalidTokenException(
                    "Token Google não contém email"
            );
        }

        User user = userRepository.findByEmailWitchRoles(email)
                .orElseGet(() -> createGoogleUser(googleJwt, email));

        String jwt = tokenService.generateToken(user);
        String refreshToken = tokenService.generateRefreshToken();

        saveRefreshToken(user, request, refreshToken);

        return new LoginResponseDTO(jwt, refreshToken);
    }

    @Transactional
    public void logout(String requestRefreshToken) {

        tokenRepository.findByToken(requestRefreshToken).ifPresent(refreshToken -> {
            refreshToken.revokeToken();
            tokenRepository.save(refreshToken);
        });
    }

    @Transactional
    public LoginResponseDTO refreshToken(String requestRefreshToken, HttpServletRequest request) {

        RefreshToken refreshToken = tokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token não encontrado"));

        if (refreshToken.getRevoked()) {
            tokenRepository.revokeAllUserTokens(refreshToken.getUser().getId());
            throw new InvalidTokenException("Tentativa de reuso de token detectada. Todos os tokens foram revogados.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expirado");
        }

        refreshToken.revokeToken();
        tokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = tokenService.generateToken(user);
        String newRefreshToken = tokenService.generateRefreshToken();

        saveRefreshToken(user, request, newRefreshToken);

        return new LoginResponseDTO(newAccessToken, newRefreshToken);
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

        createClient(user);

        return userRepository.save(user);
    }

    private void saveRefreshToken(User user, HttpServletRequest request, String token) {
        Instant expiresAt = Instant.now().plusSeconds(jwtProperties.getRefreshExpiration());

        String ipAddress = request != null ? request.getRemoteAddr() : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        tokenRepository.save(refreshToken);
    }

    private void createClient(User user) {

        Client client = Client.builder()
                .loyaltyPoints(0)
                .build();

        client.addUser(user);
    }
}
