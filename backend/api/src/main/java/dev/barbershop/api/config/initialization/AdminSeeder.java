package dev.barbershop.api.config.initialization;

import dev.barbershop.api.auth.authorization.entity.Role;
import dev.barbershop.api.auth.authorization.enums.RoleName;
import dev.barbershop.api.auth.authorization.repository.RoleRepository;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        createAdmin();
    }
    
    public void createAdmin() throws ResourceNotFoundException {
        
        if (!repository.existsByEmail(ADMIN_EMAIL)) {

            Role defaultRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma role com o nome ADMIN"));

            User admin = User.builder()
                    .name("Administrador")
                    .email(ADMIN_EMAIL)
                    .password(Objects.requireNonNull(passwordEncoder.encode(ADMIN_PASSWORD)))
                    .build();

            admin.addRole(defaultRole);

            repository.save(admin);
        }
    }
}
