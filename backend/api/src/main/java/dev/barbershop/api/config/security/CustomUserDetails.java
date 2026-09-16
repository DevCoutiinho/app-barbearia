package dev.barbershop.api.config.security;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CustomUserDetails implements UserDetails, CredentialsContainer {
    private final UUID id;
    private final String name;
    private final String email;

    private @Nullable String password;
    private final Set<GrantedAuthority> authorities;

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
