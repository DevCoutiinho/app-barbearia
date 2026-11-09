package dev.barbershop.api.config.scheduling;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SchedulingProperties.class)
public class SchedulingConfig {

    /**
     * Relógio injetável para que as regras que dependem de "agora" (horários passados,
     * janela máxima de agendamento) possam ser testadas de forma determinística.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

