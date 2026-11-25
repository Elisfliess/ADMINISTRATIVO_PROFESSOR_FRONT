package br.com.compasso.financeiro.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PeriodicidadePlanoConverter implements AttributeConverter<PeriodicidadePlano, String> {

    @Override
    public String convertToDatabaseColumn(PeriodicidadePlano atributo) {
        return atributo == null ? null : atributo.valorBanco();
    }

    @Override
    public PeriodicidadePlano convertToEntityAttribute(String valor) {
        return valor == null ? null : PeriodicidadePlano.deValorBanco(valor);
    }
}
