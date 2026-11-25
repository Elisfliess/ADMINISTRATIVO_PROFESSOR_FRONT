package br.com.compasso.financeiro.entity;

/** Situação de cobrança conforme as constraints do PostgreSQL. */
public enum CobrancaStatus {

    PENDENTE("pendente"),
    PAGO("pago"),
    CANCELADO("cancelado");

    private final String valorBanco;

    CobrancaStatus(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String valorBanco() {
        return valorBanco;
    }

    public static CobrancaStatus deValorBanco(String valor) {
        for (CobrancaStatus status : values()) {
            if (status.valorBanco.equals(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Situação de cobrança desconhecida: " + valor);
    }
}
