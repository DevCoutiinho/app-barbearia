package dev.barbershop.api.config.security;

import dev.barbershop.api.auth.authorization.enums.RoleName;
import dev.barbershop.api.common.security.AuthorityMapper;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;
    private final AuthorityMapper authorityMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = repository.findByEmailWitchRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("Nenhum usuário encontrado com esse email"));

        Set<GrantedAuthority> authorities = authorityMapper.mapToAuthorities(user);
        Set<RoleName> roles = authorityMapper.mapToRoleName(user);

        return new CustomUserDetails(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                roles,
                authorities
        );
    }
}
