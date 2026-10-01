package dev.barbershop.api.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "api.security.jwt")
public class JwtProperties {

    private String secret;
    private String googleIssuer;
    private String localIssuer;
    private Long localExpiration;
    private Long refreshExpiration;
}
