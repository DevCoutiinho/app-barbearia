package dev.barbershop.api.config.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Configurações da agenda.
 *
 * @param zoneId              fuso horário da barbearia. A grade semanal (LocalTime) e as datas
 *                            dos bloqueios são interpretadas nesse fuso.
 * @param slotIntervalMinutes passo entre os horários iniciais oferecidos ao cliente (ex.: 15 -> 08:00, 08:15...).
 * @param maxDaysAhead        quantos dias à frente o cliente pode consultar/agendar.
 */
@ConfigurationProperties(prefix = "api.scheduling")
public record SchedulingProperties(
        @DefaultValue("America/Sao_Paulo") ZoneId zoneId,
        @DefaultValue("15") int slotIntervalMinutes,
        @DefaultValue("60") int maxDaysAhead
) {

    public SchedulingProperties {
        if (slotIntervalMinutes <= 0) {
            throw new IllegalArgumentException("api.scheduling.slot-interval-minutes deve ser maior que zero");
        }
        if (maxDaysAhead <= 0) {
            throw new IllegalArgumentException("api.scheduling.max-days-ahead deve ser maior que zero");
        }
    }

    public Duration slotInterval() {
        return Duration.ofMinutes(slotIntervalMinutes);
    }
}

