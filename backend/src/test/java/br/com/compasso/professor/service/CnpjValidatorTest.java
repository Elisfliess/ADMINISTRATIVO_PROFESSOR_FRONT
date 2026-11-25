package br.com.compasso.professor.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.compasso.support.CnpjTestFactory;

class CnpjValidatorTest {

    @Test
    void aceitaCnpjNumericoComDigitosVerificadoresValidos() {
        assertThat(CnpjValidator.valido("11222333000181")).isTrue();
    }

    @Test
    void rejeitaDigitoVerificadorIncorretoEmCnpjNumerico() {
        assertThat(CnpjValidator.valido("11222333000182")).isFalse();
    }

    @Test
    void aceitaCnpjAlfanumericoOficial() {
        // Vetor divulgado pela Receita Federal para o CNPJ alfanumérico.
        assertThat(CnpjValidator.valido("12ABC34501DE35")).isTrue();
    }

    @Test
    void aceitaCnpjAlfanumericoGeradoParaTestes() {
        assertThat(CnpjValidator.valido(CnpjTestFactory.gerarAlfanumerico(123))).isTrue();
    }

    @Test
    void rejeitaDigitoVerificadorIncorretoEmCnpjAlfanumerico() {
        assertThat(CnpjValidator.valido("12ABC34501DE36")).isFalse();
        assertThat(CnpjValidator.valido("12ABC34501DE25")).isFalse();
    }

    @Test
    void rejeitaLetrasMinusculasEPontuacao() {
        assertThat(CnpjValidator.valido("12abc34501de35")).isFalse();
        assertThat(CnpjValidator.valido("12.ABC.345/01DE-35")).isFalse();
    }

    @Test
    void rejeitaCnpjComTodosOsCaracteresIguais() {
        assertThat(CnpjValidator.valido("00000000000000")).isFalse();
        assertThat(CnpjValidator.valido("11111111111111")).isFalse();
    }

    @Test
    void rejeitaTamanhoOuCaracteresInvalidos() {
        assertThat(CnpjValidator.valido("1122233300018")).isFalse();
        assertThat(CnpjValidator.valido("112223330001811")).isFalse();
        assertThat(CnpjValidator.valido("1122233300018A")).isFalse();
        assertThat(CnpjValidator.valido("12ABC34501DE3A")).isFalse();
        assertThat(CnpjValidator.valido("12ABC34501DE3!")).isFalse();
        assertThat(CnpjValidator.valido(null)).isFalse();
    }
}
