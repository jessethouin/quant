package com.jessethouin.quant.db;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

@Converter
public class BigDecimalConverter implements AttributeConverter<BigDecimal, Double> {

    @Override
    public Double convertToDatabaseColumn(BigDecimal bigDecimalValue) {
        if (bigDecimalValue == null) {
            return null;
        }

        return bigDecimalValue.doubleValue();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Double doubleValue) {
        if (doubleValue == null) {
            return null;
        }

        return BigDecimal.valueOf(doubleValue);
    }

}
