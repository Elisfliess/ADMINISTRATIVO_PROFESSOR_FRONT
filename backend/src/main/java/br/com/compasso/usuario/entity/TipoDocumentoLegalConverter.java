package br.com.compasso.usuario.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TipoDocumentoLegalConverter implements AttributeConverter<TipoDocumentoLegal, String> {

    @Override
    public String convertToDatabaseColumn(TipoDocumentoLegal atributo) {
        return atributo == null ? null : atributo.valorBanco();
    }

    @Override
    public TipoDocumentoLegal convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoDocumentoLegal.deValorBanco(valor);
    }
}
