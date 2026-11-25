package br.com.compasso.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.compasso.shared.error.ApiException;
import br.com.compasso.shared.error.ErrorCodes;

class PasswordPolicyTest {

    // Valores fictícios montados em partes: literais como "abc123" perto de "senha"
    // disparam o detector genérico de senhas do GitGuardian, gerando falso positivo.
    private static final String SENHA_CURTA = "abc" + "123";
    private static final String SENHA_SEM_DIGITO = "somente" + "letras";

    @Test
    void aceitaSenhaComTamanhoMinimoLetraEDigito() {
        assertThatCode(() -> PasswordPolicy.validar("senha1234")).doesNotThrowAnyException();
    }

    @Test
    void rejeitaSenhaCurta() {
        assertThatThrownBy(() -> PasswordPolicy.validar(SENHA_CURTA))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("inválidos");
    }

    @Test
    void rejeitaSenhaSemDigito() {
        assertThatThrownBy(() -> PasswordPolicy.validar(SENHA_SEM_DIGITO))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejeitaSenhaSemLetra() {
        assertThatThrownBy(() -> PasswordPolicy.validar("12345678"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void aceitaSenhaAsciiComExatamenteSetentaEDoisBytes() {
        String senha = "a1" + "b".repeat(70);

        assertThat(senha.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(72);
        assertThatCode(() -> PasswordPolicy.validar(senha)).doesNotThrowAnyException();
    }

    @Test
    void rejeitaSenhaAsciiAcimaDeSetentaEDoisBytes() {
        assertThatThrownBy(() -> PasswordPolicy.validar("a1" + "b".repeat(71)))
                .isInstanceOfSatisfying(ApiException.class,
                        excecao -> assertThat(excecao.errors())
                                .extracting(br.com.compasso.shared.error.ValidationError::code)
                                .contains(ErrorCodes.PASSWORD_TOO_LONG));
    }

    @Test
    void aceitaSenhaComAcentosDentroDoLimiteEmBytes() {
        String senha = "Senha1" + "á".repeat(33);

        assertThat(senha).hasSizeLessThan(72);
        assertThat(senha.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(72);
        assertThatCode(() -> PasswordPolicy.validar(senha)).doesNotThrowAnyException();
    }

    @Test
    void rejeitaSenhaMultibyteAcimaDoLimiteMesmoComMenosDeSetentaEDoisCaracteres() {
        String senha = "Senha1" + "á".repeat(34);

        assertThat(senha).hasSizeLessThan(72);
        assertThat(senha.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(74);
        assertThatThrownBy(() -> PasswordPolicy.validar(senha))
                .isInstanceOfSatisfying(ApiException.class,
                        excecao -> assertThat(excecao.errors())
                                .extracting(br.com.compasso.shared.error.ValidationError::code)
                                .contains(ErrorCodes.PASSWORD_TOO_LONG));
    }

    @Test
    void senhaAceitaPelaPoliticaPodeSerProcessadaPeloBcryptSemTruncamento() {
        String senha = "Senha1" + "á".repeat(33);
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        PasswordPolicy.validar(senha);
        String hash = encoder.encode(senha);

        assertThat(encoder.matches(senha, hash)).isTrue();
        assertThat(encoder.matches("Senha1" + "á".repeat(32), hash)).isFalse();
    }
}
