package br.com.compasso.financeiro.entity;

/** Periodicidade de plano conforme as constraints do PostgreSQL. */
public enum PeriodicidadePlano {

    MENSAL("mensal"),
    POR_AULA("por_aula");

    private final String valorBanco;

    PeriodicidadePlano(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String valorBanco() {
        return valorBanco;
    }

    public static PeriodicidadePlano deValorBanco(String valor) {
        for (PeriodicidadePlano periodicidade : values()) {
            if (periodicidade.valorBanco.equals(valor)) {
                return periodicidade;
            }
        }
        throw new IllegalArgumentException("Periodicidade de plano desconhecida: " + valor);
    }
}
