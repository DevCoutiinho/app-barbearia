package dev.barbershop.api.availability.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

/**
 * Persiste {@link DayOfWeek} como inteiro ISO-8601 (1 = segunda ... 7 = domingo).
 */
@Converter
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : DayOfWeek.of(dbData);
    }
}

