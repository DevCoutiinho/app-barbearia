package dev.barbershop.api.config.security;

import dev.barbershop.api.auth.authorization.enums.RoleName;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.common.security.AuthorityMapper;
import dev.barbershop.api.user.entity.User;
import dev.barbershop.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailsJwtPrincipalConverter implements Converter<Jwt, OAuth2AuthenticatedPrincipal> {

    private final UserRepository repository;
    private final AuthorityMapper authorityMapper;

    @Override
    public OAuth2AuthenticatedPrincipal convert(Jwt jwt) {

        String email = jwt.getClaimAsString("email");

        User user = repository.findByEmailWitchRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Set<GrantedAuthority> authorities = authorityMapper.mapToAuthorities(user);
        Set<RoleName> roles = authorityMapper.mapToRoleName(user);

        System.out.println("authorities : " + authorities);

        CustomUserDetails customUserDetails = new CustomUserDetails(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                roles,
                authorities
        );

        return new JwtUser(jwt, customUserDetails);
    }

    private static final class JwtUser extends CustomUserDetails implements OAuth2AuthenticatedPrincipal {
        private final Jwt jwt;

        private JwtUser(Jwt jwt, CustomUserDetails user) {
            super(user.getId(), user.getName(), user.getEmail(), user.getPassword(), user.getRoles(), user.getAuthorities());
            this.jwt = jwt;
        }

        @Override
        public String getName() {
            return this.jwt.getSubject();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return this.jwt.getClaims();
        }
    }
}