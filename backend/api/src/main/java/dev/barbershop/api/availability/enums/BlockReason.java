package dev.barbershop.api.availability.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlockReason {
    LUNCH("Almoço"),
    DAY_OFF("Folga"),
    HOLIDAY("Feriado"),
    PERSONAL_APPOINTMENT("Compromisso pessoal"),
    LEAVE("Saída");

    private final String label;
}

