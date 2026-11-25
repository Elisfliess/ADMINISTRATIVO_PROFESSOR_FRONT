package br.com.compasso.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.compasso.professor.service.CnpjValidator;
import br.com.compasso.professor.service.ProfessorService;
import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.shared.error.ValidationError;
import br.com.compasso.usuario.dto.RegisterRequest;
import br.com.compasso.usuario.entity.Usuario;
import br.com.compasso.usuario.service.ContaService;
import br.com.compasso.usuario.service.UsuarioService;

/**
 * Cadastro coordenado de conta ({@code usuario}) e perfil inicial
 * ({@code professor}) em uma única transação. O envio do e-mail de verificação
 * ocorre somente após o commit. A resposta HTTP é genérica para não permitir
 * enumeração de contas.
 */
@Service
public class CadastroProfessorUseCase {

    private static final Logger log = LoggerFactory.getLogger(CadastroProfessorUseCase.class);

    private final UsuarioService usuarioService;
    private final ProfessorService professorService;
    private final ContaService contaService;

    public CadastroProfessorUseCase(UsuarioService usuarioService, ProfessorService professorService,
            ContaService contaService) {
        this.usuarioService = usuarioService;
        this.professorService = professorService;
        this.contaService = contaService;
    }

    @Transactional
    public void registrar(RegisterRequest requisicao) {
        String email = UsuarioService.normalizarEmail(requisicao.email());
        String cnpj = requisicao.cnpj();

        if (!CnpjValidator.valido(cnpj)) {
            throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", List.of(
                    new ValidationError("cnpj", ErrorCodes.CNPJ_INVALID, "Informe um CNPJ válido.")));
        }

        if (usuarioService.buscarPorEmail(email).isPresent()) {
            contaService.enviarAvisoDeEmailExistente(email);
            return;
        }

        if (professorService.cnpjJaCadastrado(cnpj)) {
            log.warn("Cadastro descartado: CNPJ já vinculado a outro professor.");
            return;
        }

        Usuario usuario = usuarioService.criarConta(email, requisicao.senha());

        professorService.criar(
                usuario.getId(),
                requisicao.nome().trim(),
                requisicao.razaoSocial().trim(),
                cnpj,
                opcional(requisicao.emailContato()),
                opcional(requisicao.telefoneContato()));

        contaService.enviarEmailDeVerificacao(usuario);
    }

    private String opcional(String valor) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
