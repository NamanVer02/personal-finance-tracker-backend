package com.example.personal_finance_tracker.app.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Base64;

@Converter(autoApply = false)
public class DoubleEncodeConverter implements AttributeConverter<Double, Double> {
    @Override
    public Double convertToDatabaseColumn(Double attribute) {
        if (attribute == null) return null;
        // Convert to string, encode, then parse back to double
        String encoded = Base64.getEncoder().encodeToString(attribute.toString().getBytes());
        return Double.parseDouble(encoded);
    }

    @Override
    public Double convertToEntityAttribute(Double dbData) {
        if (dbData == null) return null;
        // Convert to string, decode, then parse back to double
        String decoded = new String(Base64.getDecoder().decode(dbData.toString().getBytes()));
        return Double.parseDouble(decoded);
    }
}