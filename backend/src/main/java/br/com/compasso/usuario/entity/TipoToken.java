package br.com.compasso.usuario.entity;

/** Tipo de token de conta conforme a constraint {@code ck_token_conta_tipo}. */
public enum TipoToken {

    VERIFICACAO_EMAIL("verificacao_email"),
    RECUPERACAO_SENHA("recuperacao_senha");

    private final String valorBanco;

    TipoToken(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String valorBanco() {
        return valorBanco;
    }

    public static TipoToken deValorBanco(String valor) {
        for (TipoToken tipo : values()) {
            if (tipo.valorBanco.equals(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de token desconhecido: " + valor);
    }
}
