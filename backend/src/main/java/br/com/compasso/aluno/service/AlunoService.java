package br.com.compasso.aluno.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.compasso.aluno.dto.AlunoAnonimizacaoPreviaResponse;
import br.com.compasso.aluno.dto.AlunoAnonimizacaoRequest;
import br.com.compasso.aluno.dto.AlunoCreateRequest;
import br.com.compasso.aluno.dto.AlunoCsv;
import br.com.compasso.aluno.dto.AlunoPatchRequest;
import br.com.compasso.aluno.dto.ResponsavelLegalPatchRequest;
import br.com.compasso.aluno.dto.ResponsavelLegalRequest;
import br.com.compasso.aluno.entity.Aluno;
import br.com.compasso.aluno.repository.AlunoRepository;
import br.com.compasso.professor.entity.Professor;
import br.com.compasso.professor.service.ProfessorService;
import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.shared.error.ValidationError;
import br.com.compasso.usuario.service.AutenticacaoService;

/**
 * Regras do módulo aluno. Todas as operações resolvem o professor pela sessão
 * autenticada e restringem qualquer consulta ao escopo desse professor; aluno
 * de outro professor é tratado como inexistente (404).
 */
@Service
public class AlunoService {

    private static final ZoneId ZONA_CALENDARIO = ZoneId.of("America/Sao_Paulo");
    private static final int IDADE_MAIORIDADE = 18;

    private static final List<String> COLUNAS_CSV = List.of(
            "id", "nome", "telefone", "email", "dataNascimento", "observacoes",
            "responsavelNome", "responsavelContato", "ativo", "inativadoEm",
            "anonimizadoEm", "criadoEm", "atualizadoEm");

    private static final List<String> REGISTROS_PRESERVADOS = List.of(
            "identificador do aluno", "professor", "aulas", "planos", "cobranças");

    /** Terminador de registro exigido pelo RFC 4180. */
    private static final String TERMINADOR_LINHA = "\r\n";

    /** Inícios de célula que planilhas podem interpretar como fórmula. */
    private static final String MARCADORES_FORMULA = "=+-@";

    private final AlunoRepository alunoRepository;
    private final ProfessorService professorService;
    private final AutenticacaoService autenticacaoService;
    private final Clock clock;

    public AlunoService(AlunoRepository alunoRepository, ProfessorService professorService,
            AutenticacaoService autenticacaoService, Clock clock) {
        this.alunoRepository = alunoRepository;
        this.professorService = professorService;
        this.autenticacaoService = autenticacaoService;
        this.clock = clock;
    }

    @Transactional
    public Aluno criar(UUID usuarioId, AlunoCreateRequest requisicao) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);

        List<ValidationError> erros = new ArrayList<>();
        validarResponsavel(requisicao.dataNascimento(), requisicao.responsavelLegal(), erros);
        if (!erros.isEmpty()) {
            throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", erros);
        }

        ResponsavelLegalRequest responsavel = requisicao.responsavelLegal();
        Instant agora = clock.instant();

        Aluno aluno = new Aluno(
                professor.getId(),
                requisicao.nome().trim(),
                requisicao.telefone().trim(),
                requisicao.dataNascimento(),
                opcional(requisicao.email()),
                opcional(requisicao.observacoes()),
                responsavel == null ? null : responsavel.nome().trim(),
                responsavel == null ? null : responsavel.contato().trim(),
                responsavel == null ? null : agora);

        return alunoRepository.save(aluno);
    }

    @Transactional(readOnly = true)
    public Page<Aluno> listar(UUID usuarioId, String nome, Boolean ativo, Pageable pageable) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        String filtroNome = opcional(nome);
        boolean somenteAtivos = ativo == null || ativo;

        if (filtroNome == null) {
            return alunoRepository.findByProfessorIdAndAtivo(professor.getId(), somenteAtivos, pageable);
        }
        return alunoRepository.findByProfessorIdAndNomeContainingIgnoreCaseAndAtivo(professor.getId(), filtroNome,
                somenteAtivos, pageable);
    }

    @Transactional(readOnly = true)
    public Aluno obter(UUID usuarioId, UUID alunoId) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        return buscarDoProfessor(professor.getId(), alunoId);
    }

    @Transactional
    public Aluno atualizar(UUID usuarioId, UUID alunoId, AlunoPatchRequest patch) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        Aluno aluno = buscarDoProfessor(professor.getId(), alunoId);
        if (aluno.isAnonimizado()) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "Aluno anonimizado não pode ser alterado.");
        }

        String nome = patch.possui("nome") ? patch.getNome() : aluno.getNome();
        String telefone = patch.possui("telefone") ? patch.getTelefone() : aluno.getTelefone();
        LocalDate dataNascimento = patch.possui("dataNascimento")
                ? patch.getDataNascimento()
                : aluno.getDataNascimento();
        String email = patch.possui("email") ? opcional(patch.getEmail()) : aluno.getEmail();
        String observacoes = patch.possui("observacoes") ? opcional(patch.getObservacoes()) : aluno.getObservacoes();
        ResponsavelLegalRequest responsavel = responsavelMesclado(aluno, patch);

        List<ValidationError> erros = new ArrayList<>();
        exigirTexto(erros, "nome", nome, "O nome é obrigatório.");
        exigirTexto(erros, "telefone", telefone, "O telefone é obrigatório.");
        if (dataNascimento == null) {
            erros.add(new ValidationError("dataNascimento", "REQUIRED", "A data de nascimento é obrigatória."));
        }
        validarDadosResponsavel(responsavel, erros);
        validarResponsavel(dataNascimento, responsavel, erros);
        if (!erros.isEmpty()) {
            throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", erros);
        }

        boolean responsavelAlterado = patch.possui("responsavelLegal")
                && patch.getResponsavelLegal() != null
                && patch.getResponsavelLegal().possuiDados();

        Instant informadoEm;
        if (responsavel == null) {
            informadoEm = null;
        } else if (responsavelAlterado) {
            informadoEm = clock.instant();
        } else {
            informadoEm = aluno.getResponsavelInformadoEm();
        }

        aluno.atualizarDados(
                nome.trim(),
                telefone.trim(),
                dataNascimento,
                email,
                observacoes,
                responsavel == null ? null : responsavel.nome().trim(),
                responsavel == null ? null : responsavel.contato().trim(),
                informadoEm);

        return alunoRepository.save(aluno);
    }

    /**
     * Reativa o aluno. A operação altera somente o estado do cadastro; aulas
     * canceladas e plano encerrado permanecem como estão.
     */
    @Transactional
    public void reativar(UUID usuarioId, UUID alunoId) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        Aluno aluno = buscarDoProfessor(professor.getId(), alunoId);

        if (aluno.isAnonimizado()) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "Aluno anonimizado não pode ser reativado.");
        }
        if (aluno.isAtivo()) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "O aluno já está ativo.");
        }

        aluno.reativar();
        alunoRepository.save(aluno);
    }

    /** Exporta os dados pessoais do aluno em CSV após confirmar a senha (RF39). */
    @Transactional(readOnly = true)
    public AlunoCsv exportarCsv(UUID usuarioId, UUID alunoId, String senha) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        Aluno aluno = buscarDoProfessor(professor.getId(), alunoId);
        exigirSenha(usuarioId, senha);

        byte[] conteudo = montarCsv(aluno).getBytes(StandardCharsets.UTF_8);
        return new AlunoCsv("aluno-" + aluno.getId() + ".csv", conteudo);
    }

    /** Descreve os dados que serão anonimizados sem alterar o cadastro (RF40). */
    @Transactional(readOnly = true)
    public AlunoAnonimizacaoPreviaResponse previaAnonimizacao(UUID usuarioId, UUID alunoId) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        Aluno aluno = buscarDoProfessor(professor.getId(), alunoId);

        List<String> dadosPessoais = new ArrayList<>();
        if (!aluno.isAnonimizado()) {
            dadosPessoais.add("nome");
            adicionarSePresente(dadosPessoais, "telefone", aluno.getTelefone());
            adicionarSePresente(dadosPessoais, "email", aluno.getEmail());
            adicionarSePresente(dadosPessoais, "dataNascimento", aluno.getDataNascimento());
            adicionarSePresente(dadosPessoais, "observacoes", aluno.getObservacoes());
            adicionarSePresente(dadosPessoais, "responsavelLegal.nome", aluno.getResponsavelNome());
            adicionarSePresente(dadosPessoais, "responsavelLegal.contato", aluno.getResponsavelContato());
            adicionarSePresente(dadosPessoais, "responsavelLegal.informadoEm", aluno.getResponsavelInformadoEm());
        }

        return new AlunoAnonimizacaoPreviaResponse(dadosPessoais, REGISTROS_PRESERVADOS);
    }

    /**
     * Anonimiza os dados pessoais preservando identificador, vínculo e registros
     * históricos. Exige senha e confirmação explícita (RF40).
     */
    @Transactional
    public void anonimizar(UUID usuarioId, UUID alunoId, AlunoAnonimizacaoRequest requisicao) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        Aluno aluno = buscarDoProfessor(professor.getId(), alunoId);
        exigirSenha(usuarioId, requisicao.senha());

        if (aluno.isAnonimizado()) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "Os dados do aluno já foram anonimizados.");
        }

        aluno.anonimizar(clock.instant());
        alunoRepository.save(aluno);
    }

    private void exigirSenha(UUID usuarioId, String senha) {
        if (!autenticacaoService.confirmarSenha(usuarioId, senha)) {
            throw Exceptions.acessoNegado(ErrorCodes.FORBIDDEN, "Senha inválida.");
        }
    }

    private String montarCsv(Aluno aluno) {
        List<String> valores = Arrays.asList(
                texto(aluno.getId()),
                texto(aluno.getNome()),
                texto(aluno.getTelefone()),
                texto(aluno.getEmail()),
                texto(aluno.getDataNascimento()),
                texto(aluno.getObservacoes()),
                texto(aluno.getResponsavelNome()),
                texto(aluno.getResponsavelContato()),
                Boolean.toString(aluno.isAtivo()),
                texto(aluno.getInativadoEm()),
                texto(aluno.getAnonimizadoEm()),
                texto(aluno.getCriadoEm()),
                texto(aluno.getAtualizadoEm()));

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", COLUNAS_CSV)).append(TERMINADOR_LINHA);
        csv.append(String.join(",", valores.stream().map(this::campoCsv).toList())).append(TERMINADOR_LINHA);
        return csv.toString();
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    /**
     * Serializa um campo segundo o RFC 4180 e neutraliza fórmulas de planilha. O
     * valor original permanece intacto no banco; somente a representação
     * exportada recebe um apóstrofo quando o primeiro caractere significativo é
     * {@code =}, {@code +}, {@code -} ou {@code @}, ignorando espaços e
     * caracteres de controle anteriores. Aspas, vírgulas e quebras de linha
     * continuam escapadas pelo padrão.
     */
    private String campoCsv(String valor) {
        if (valor == null) {
            return "";
        }
        String conteudo = neutralizarFormula(valor);
        boolean precisaAspas = conteudo.indexOf(',') >= 0 || conteudo.indexOf('"') >= 0
                || conteudo.indexOf('\n') >= 0 || conteudo.indexOf('\r') >= 0;
        if (!precisaAspas) {
            return conteudo;
        }
        return '"' + conteudo.replace("\"", "\"\"") + '"';
    }

    private String neutralizarFormula(String valor) {
        int inicio = 0;
        while (inicio < valor.length() && ehEspacoOuControle(valor.charAt(inicio))) {
            inicio++;
        }
        if (inicio < valor.length() && MARCADORES_FORMULA.indexOf(valor.charAt(inicio)) >= 0) {
            return "'" + valor;
        }
        return valor;
    }

    private boolean ehEspacoOuControle(char caractere) {
        return Character.isWhitespace(caractere) || Character.isSpaceChar(caractere)
                || Character.isISOControl(caractere);
    }

    private void adicionarSePresente(List<String> campos, String campo, Object valor) {
        if (valor != null) {
            campos.add(campo);
        }
    }

    private Aluno buscarDoProfessor(UUID professorId, UUID alunoId) {
        return alunoRepository.buscarPorIdEProfessor(alunoId, professorId)
                .orElseThrow(() -> Exceptions.naoEncontrado("Aluno não encontrado."));
    }

    private ResponsavelLegalRequest responsavelMesclado(Aluno aluno, AlunoPatchRequest patch) {
        if (!patch.possui("responsavelLegal")) {
            return responsavelAtual(aluno);
        }

        ResponsavelLegalPatchRequest alteracao = patch.getResponsavelLegal();
        if (alteracao == null) {
            return null;
        }
        if (!alteracao.possuiDados()) {
            return responsavelAtual(aluno);
        }

        String nome = alteracao.possui("nome") ? alteracao.getNome() : aluno.getResponsavelNome();
        String contato = alteracao.possui("contato") ? alteracao.getContato() : aluno.getResponsavelContato();
        return new ResponsavelLegalRequest(nome, contato);
    }

    private ResponsavelLegalRequest responsavelAtual(Aluno aluno) {
        if (aluno.getResponsavelNome() == null && aluno.getResponsavelContato() == null) {
            return null;
        }
        return new ResponsavelLegalRequest(aluno.getResponsavelNome(), aluno.getResponsavelContato());
    }

    private void validarDadosResponsavel(ResponsavelLegalRequest responsavel, List<ValidationError> erros) {
        if (responsavel == null) {
            return;
        }
        exigirTexto(erros, "responsavelLegal.nome", responsavel.nome(),
                "O nome do responsável legal é obrigatório.");
        exigirTexto(erros, "responsavelLegal.contato", responsavel.contato(),
                "O contato do responsável legal é obrigatório.");
    }

    private void validarResponsavel(LocalDate dataNascimento, ResponsavelLegalRequest responsavel,
            List<ValidationError> erros) {
        if (dataNascimento != null && menorDeIdade(dataNascimento) && responsavel == null) {
            erros.add(new ValidationError("responsavelLegal", "REQUIRED",
                    "O responsável legal é obrigatório para alunos menores de 18 anos."));
        }
    }

    private boolean menorDeIdade(LocalDate dataNascimento) {
        LocalDate hoje = LocalDate.now(clock.withZone(ZONA_CALENDARIO));
        return Period.between(dataNascimento, hoje).getYears() < IDADE_MAIORIDADE;
    }

    private void exigirTexto(List<ValidationError> erros, String campo, String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            erros.add(new ValidationError(campo, "REQUIRED", mensagem));
        }
    }

    private String opcional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
