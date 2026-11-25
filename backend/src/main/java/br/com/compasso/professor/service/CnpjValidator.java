package br.com.compasso.professor.service;

/**
 * Validação de CNPJ conforme A14 e a especificação alfanumérica da Receita
 * Federal (vigente a partir de 31/07/2026): 12 primeiras posições alfanuméricas
 * (0-9 ou A-Z) e 2 dígitos verificadores numéricos.
 *
 * <p>O valor de cada caractere para o módulo 11 é o código ASCII menos 48,
 * portanto dígitos mantêm o valor usual (0-9) e letras valem A=17 até Z=42.
 * Isso preserva o cálculo dos CNPJs numéricos legados.</p>
 */
public final class CnpjValidator {

    private static final int[] PESOS_PRIMEIRO_DIGITO = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
    private static final int[] PESOS_SEGUNDO_DIGITO = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };

    private CnpjValidator() {
    }

    public static boolean valido(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return false;
        }

        for (int i = 0; i < 12; i++) {
            if (!alfanumerico(cnpj.charAt(i))) {
                return false;
            }
        }

        if (!digito(cnpj.charAt(12)) || !digito(cnpj.charAt(13))) {
            return false;
        }

        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }

        int primeiroDigito = digitoVerificador(cnpj, PESOS_PRIMEIRO_DIGITO);
        int segundoDigito = digitoVerificador(cnpj, PESOS_SEGUNDO_DIGITO);

        return cnpj.charAt(12) - '0' == primeiroDigito && cnpj.charAt(13) - '0' == segundoDigito;
    }

    private static boolean alfanumerico(char caractere) {
        return digito(caractere) || (caractere >= 'A' && caractere <= 'Z');
    }

    private static boolean digito(char caractere) {
        return caractere >= '0' && caractere <= '9';
    }

    private static int digitoVerificador(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            // ASCII - 48: '0'..'9' => 0..9; 'A'..'Z' => 17..42.
            soma += (cnpj.charAt(i) - 48) * pesos[i];
        }

        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
