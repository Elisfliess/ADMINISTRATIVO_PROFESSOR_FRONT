package br.com.compasso.usuario.entity;

/** Tipo de documento legal conforme a constraint {@code ck_versao_documento_tipo}. */
public enum TipoDocumentoLegal {

    TERMOS_USO("termos_uso"),
    POLITICA_PRIVACIDADE("politica_privacidade");

    private final String valorBanco;

    TipoDocumentoLegal(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String valorBanco() {
        return valorBanco;
    }

    public static TipoDocumentoLegal deValorBanco(String valor) {
        for (TipoDocumentoLegal tipo : values()) {
            if (tipo.valorBanco.equals(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de documento legal desconhecido: " + valor);
    }
}
