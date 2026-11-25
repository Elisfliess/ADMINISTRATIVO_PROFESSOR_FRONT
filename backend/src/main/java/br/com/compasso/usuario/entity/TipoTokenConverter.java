package br.com.compasso.usuario.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TipoTokenConverter implements AttributeConverter<TipoToken, String> {

    @Override
    public String convertToDatabaseColumn(TipoToken atributo) {
        return atributo == null ? null : atributo.valorBanco();
    }

    @Override
    public TipoToken convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoToken.deValorBanco(valor);
    }
}
