package br.com.compasso.financeiro.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CobrancaStatusConverter implements AttributeConverter<CobrancaStatus, String> {

    @Override
    public String convertToDatabaseColumn(CobrancaStatus atributo) {
        return atributo == null ? null : atributo.valorBanco();
    }

    @Override
    public CobrancaStatus convertToEntityAttribute(String valor) {
        return valor == null ? null : CobrancaStatus.deValorBanco(valor);
    }
}
