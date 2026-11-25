package br.com.compasso.support;

/** Gera CNPJs fictícios válidos para testes, sem depender do validador de produção. */
public final class CnpjTestFactory {

    private static final int[] PESOS_PRIMEIRO = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
    private static final int[] PESOS_SEGUNDO = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };

    private CnpjTestFactory() {
    }

    public static String gerar(long semente) {
        String base = String.format("%08d0001", Math.abs(semente) % 100_000_000L);
        return completar(base);
    }

    /**
     * Gera um CNPJ alfanumérico fictício. O prefixo fixo garante letras nas
     * primeiras posições e os dígitos finais são calculados pela mesma regra
     * oficial (ASCII - 48).
     */
    public static String gerarAlfanumerico(long semente) {
        String base = "12ABC" + String.format("%07d", Math.abs(semente) % 10_000_000L);
        return completar(base);
    }

    private static String completar(String base) {
        int primeiro = digito(base, PESOS_PRIMEIRO);
        int segundo = digito(base + primeiro, PESOS_SEGUNDO);
        return base + primeiro + segundo;
    }

    private static int digito(String caracteres, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (caracteres.charAt(i) - 48) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
