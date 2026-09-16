package dev.barbershop.api.common.security;

import dev.barbershop.api.user.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AuthorityMapper {

    public Set<GrantedAuthority> map(User user) {
        return user.getRoles()
                .stream()
                .flatMap(role -> {
                    Stream<GrantedAuthority> roleAuthority =
                            Stream.of(new SimpleGrantedAuthority(role.getName().name()));

                    Stream<GrantedAuthority> permissionAuthorities =
                            role.getPermissions()
                                    .stream()
                                    .map(permission -> new SimpleGrantedAuthority(permission.getName().name()));

                    return Stream.concat(roleAuthority, permissionAuthorities);

                }).collect(Collectors.toSet());
    }
}
