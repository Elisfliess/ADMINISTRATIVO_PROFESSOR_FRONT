package br.com.compasso.financeiro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

import br.com.compasso.aluno.entity.Aluno;
import br.com.compasso.aluno.service.AlunoService;
import java.math.BigDecimal;
import br.com.compasso.financeiro.dto.CobrancaBaixaRequest;
import br.com.compasso.financeiro.dto.CobrancaCsv;
import br.com.compasso.financeiro.dto.CobrancaResponse;
import br.com.compasso.financeiro.dto.PlanoCreateRequest;
import br.com.compasso.financeiro.dto.PlanoPatchRequest;
import br.com.compasso.financeiro.dto.PlanoResponse;
import br.com.compasso.financeiro.entity.Cobranca;
import br.com.compasso.financeiro.entity.CobrancaStatus;
import br.com.compasso.financeiro.entity.PeriodicidadePlano;
import br.com.compasso.financeiro.entity.PlanoPagamento;
import br.com.compasso.financeiro.repository.CobrancaRepository;
import br.com.compasso.financeiro.repository.PlanoPagamentoRepository;
import br.com.compasso.professor.entity.Professor;
import br.com.compasso.professor.service.ProfessorService;
import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.uuid.UuidV7;
import br.com.compasso.usuario.service.AutenticacaoService;
import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.shared.error.ValidationError;
import br.com.compasso.shared.web.PageResponse;

/**
 * Regras do módulo financeiro: planos de pagamento e cobranças. As operações
 * resolvem o professor pela sessão autenticada, tratam aluno de outro professor
 * como inexistente (404) e restringem alterações ao escopo do proprietário.
 */
@Service
public class FinanceiroService implements GeracaoCobrancaService {

    private static final ZoneId ZONA_CALENDARIO = ZoneId.of("America/Sao_Paulo");
    private static final Pattern MES_PATTERN = Pattern.compile("^([0-9]{4})-(0[1-9]|1[0-2])$");

    private final ProfessorService professorService;
    private final AlunoService alunoService;
    private final PlanoPagamentoRepository planoRepository;
    private final CobrancaRepository cobrancaRepository;
    private final AutenticacaoService autenticacaoService;
    private final Clock clock;
    private final EntityManager entityManager;

    public FinanceiroService(ProfessorService professorService,
            AlunoService alunoService,
            PlanoPagamentoRepository planoRepository,
            CobrancaRepository cobrancaRepository,
            AutenticacaoService autenticacaoService,
            Clock clock,
            EntityManager entityManager) {
        this.professorService = professorService;
        this.alunoService = alunoService;
        this.planoRepository = planoRepository;
        this.cobrancaRepository = cobrancaRepository;
        this.autenticacaoService = autenticacaoService;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional
    public PlanoResponse criar(UUID usuarioId, UUID alunoId, PlanoCreateRequest requisicao) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        Aluno aluno = alunoService.obter(usuarioId, alunoId);

        if (planoRepository.findByAlunoIdAndProfessorIdAndEncerradoEmIsNullForUpdate(alunoId, professor.getId()).isPresent()) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT,
                    "O aluno já possui um plano ativo. Para nova contratação, encerre o plano anterior.");
        }

        validarConfiguracao(requisicao.periodicidade(), requisicao.valor(), requisicao.inicioEm(), requisicao.diaVencimento(),
                requisicao.prazoVencimentoDias());

        PlanoPagamento plano = new PlanoPagamento(
                professor.getId(),
                aluno.getId(),
                requisicao.periodicidade(),
                requisicao.valor(),
                requisicao.inicioEm(),
                requisicao.diaVencimento(),
                requisicao.prazoVencimentoDias());

        try {
            // flush imediato para capturar violação de unicidade dentro do try e não continuar após erro de constraint
            return PlanoResponse.de(planoRepository.saveAndFlush(plano));
        } catch (DataIntegrityViolationException e) {
            if (violacaoPlanoAtivoDuplicado(e)) {
                throw Exceptions.conflito(ErrorCodes.CONFLICT,
                        "O aluno já possui um plano ativo. Para nova contratação, encerre o plano anterior.");
            }
            throw e; // outras violações de integridade não são convertidas em conflito de plano ativo
        }
    }

    private boolean violacaoPlanoAtivoDuplicado(DataIntegrityViolationException e) {
        Throwable atual = e;
        while (atual != null) {
            if (atual instanceof org.hibernate.exception.ConstraintViolationException cve) {
                if ("23505".equals(cve.getSQLState()) && "uq_plano_ativo_por_aluno".equals(cve.getConstraintName())) {
                    return true;
                }
            }
            String msg = atual.getMessage();
            if (msg != null && msg.contains("uq_plano_ativo_por_aluno")) {
                return true;
            }
            atual = atual.getCause();
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<PlanoResponse> listar(UUID usuarioId, UUID alunoId) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        alunoService.obter(usuarioId, alunoId);

        return planoRepository.findByAlunoIdAndProfessorId(alunoId, professor.getId()).stream()
                .map(PlanoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanoResponse obter(UUID usuarioId, UUID planoId) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        PlanoPagamento plano = planoRepository.findByIdAndProfessorId(planoId, professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Plano não encontrado."));

        return PlanoResponse.de(plano);
    }

    @Transactional
    public PlanoResponse atualizar(UUID usuarioId, UUID planoId, PlanoPatchRequest patch) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        PlanoPagamento plano = planoRepository.findByIdAndProfessorIdForUpdate(planoId, professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Plano não encontrado."));

        if (plano.getEncerradoEm() != null) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT,
                    "As configurações de um plano encerrado são imutáveis.");
        }

        if (cobrancaRepository.existsByPlanoId(plano.getId())) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT,
                    "As configurações de um plano com cobranças não podem ser alteradas. Encerre-o e crie outro plano.");
        }

        boolean camposAlteraveis = patch.possui("periodicidade") || patch.possui("valor")
                || patch.possui("inicioEm") || patch.possui("diaVencimento") || patch.possui("prazoVencimentoDias");
        if (camposAlteraveis) {
            // Compute final state without modifying entity first
            PeriodicidadePlano periodicidadeFinal = plano.getPeriodicidade();
            if (patch.possui("periodicidade")) {
                if (patch.getPeriodicidade() == null) {
                    throw Exceptions.requisicaoInvalida("A periodicidade não pode ser nula.",
                            List.of(new ValidationError("periodicidade", "INVALID_VALUE",
                                    "A periodicidade deve ser informada.")));
                }
                periodicidadeFinal = PeriodicidadePlano.valueOf(patch.getPeriodicidade());
            }
            BigDecimal valorFinal = plano.getValor();
            if (patch.possui("valor")) {
                if (patch.getValor() == null) {
                    throw Exceptions.requisicaoInvalida("O valor não pode ser nulo.",
                            List.of(new ValidationError("valor", "INVALID_VALUE",
                                    "O valor deve ser informado.")));
                }
                valorFinal = patch.getValor();
            }
            LocalDate inicioFinal = plano.getInicioEm();
            if (patch.possui("inicioEm")) {
                if (patch.getInicioEm() == null) {
                    throw Exceptions.requisicaoInvalida("A data de início não pode ser nula.",
                            List.of(new ValidationError("inicioEm", "INVALID_VALUE",
                                    "A data de início deve ser informada.")));
                }
                inicioFinal = patch.getInicioEm();
            }
            Integer diaVencimentoFinal = plano.getDiaVencimento();
            if (patch.possui("diaVencimento")) {
                // Present with null clears the value
                diaVencimentoFinal = patch.getDiaVencimento();
            }
            Integer prazoFinal = plano.getPrazoVencimentoDias();
            if (patch.possui("prazoVencimentoDias")) {
                // Present with null clears the value
                prazoFinal = patch.getPrazoVencimentoDias();
            }
            // Validate final state before applying
            validarConfiguracao(periodicidadeFinal, valorFinal, inicioFinal, diaVencimentoFinal, prazoFinal);
            plano.alterarConfiguracao(periodicidadeFinal, valorFinal, inicioFinal, diaVencimentoFinal, prazoFinal);
        }

        return PlanoResponse.de(planoRepository.save(plano));
    }

    @Transactional
    public void encerrar(UUID usuarioId, UUID planoId) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        PlanoPagamento plano = planoRepository.findByIdAndProfessorIdForUpdate(planoId, professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Plano não encontrado."));

        if (plano.getEncerradoEm() != null) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "O plano já está encerrado.");
        }

        plano.encerrar(LocalDate.now(clock.withZone(ZONA_CALENDARIO)));
        planoRepository.save(plano);
    }

    @Transactional(readOnly = true)
    public PageResponse<CobrancaResponse> listarCobrancas(UUID usuarioId,
            String mes,
            String status,
            UUID alunoId,
            Pageable pageable) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);

        LocalDate[] periodo = mes == null ? null : mesDo(mes);
        CobrancaStatus situacao = status == null ? null : statusDo(status);

        Specification<Cobranca> filtro = filtroDeListagem(professor.getId(), situacao, alunoId,
                periodo == null ? null : periodo[0], periodo == null ? null : periodo[1]);

        Page<Cobranca> pagina = cobrancaRepository.findAll(filtro, pageable);

        return PageResponse.de(pagina, c -> CobrancaResponse.de(c, clock));
    }

    /**
     * Monta a consulta dinâmica de cobranças do professor. Somente os filtros
     * informados entram na cláusula, evitando predicados opcionais em parâmetros
     * de SQL não tipados.
     */
    private Specification<Cobranca> filtroDeListagem(UUID professorId,
            CobrancaStatus status,
            UUID alunoId,
            LocalDate inicioEm,
            LocalDate fimEm) {
        return (raiz, consulta, criterio) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(criterio.equal(raiz.get("professorId"), professorId));
            if (status != null) {
                predicados.add(criterio.equal(raiz.get("status"), status));
            }
            if (alunoId != null) {
                predicados.add(criterio.equal(raiz.get("alunoId"), alunoId));
            }
            if (inicioEm != null && fimEm != null) {
                predicados.add(criterio.between(raiz.get("vencimentoEm"), inicioEm, fimEm));
            }
            return criterio.and(predicados.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public CobrancaResponse obterCobranca(UUID usuarioId, UUID cobrancaId) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);
        Cobranca cobranca = cobrancaRepository.findByIdAndProfessorId(cobrancaId, professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Cobranca não encontrada."));

        return CobrancaResponse.de(cobranca, clock);
    }

    @Transactional
    public CobrancaResponse baixar(UUID usuarioId, UUID cobrancaId, CobrancaBaixaRequest requisicao) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);

        // Step 1: Busca cobrança para obter plano/aula (escopo do professor).
        Cobranca cobrancaInicial = cobrancaRepository.findByIdAndProfessorId(cobrancaId, professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Cobranca não encontrada."));

        // Step 2: Lock do plano primeiro (ordem de aplicação: Plano → Aula → Cobrança).
        planoRepository.findByIdAndProfessorIdForUpdate(cobrancaInicial.getPlanoId(), professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Plano não encontrado."));

        // Step 3 (Aula): tentativa de lock explícito no banco entre Plano e Cobrança quando aplicável
        // (integração mínima sem dependência Java do módulo Agenda). Revalida vínculo após locks.
        bloquearAulaSeAplicavel(cobrancaInicial.getAulaId(), cobrancaInicial.getAlunoId(), professor.getId());

        // Step 4: Lock da cobrança.
        Cobranca cobranca = cobrancaRepository.findByIdAndProfessorIdForUpdate(cobrancaId, professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Cobranca não encontrada."));
        if (!java.util.Objects.equals(cobranca.getAulaId(), cobrancaInicial.getAulaId())) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "A cobrança foi alterada durante a operação.");
        }

        // Step 5: Revalidate after locks
        if (cobranca.getStatus() != CobrancaStatus.PENDENTE) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "A baixa só pode ser registrada para uma cobrança pendente.");
        }

        LocalDate hoje = LocalDate.now(clock.withZone(ZONA_CALENDARIO));
        if (requisicao.recebidoEm().isAfter(hoje)) {
            throw Exceptions.requisicaoInvalida("A data de recebimento não pode ser futura.",
                    List.of(new ValidationError("recebidoEm", "INVALID_VALUE",
                            "A data de recebimento não pode ser posterior à data atual.")));
        }
        cobranca.baixar(requisicao.recebidoEm(), requisicao.formaPagamento());
        return CobrancaResponse.de(cobrancaRepository.save(cobranca), clock);
    }

    @Transactional
    public void cancelar(UUID usuarioId, UUID cobrancaId) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);

        // Step 1: Busca cobrança para obter plano/aula (escopo do professor).
        Cobranca cobrancaInicial = cobrancaRepository.findByIdAndProfessorId(cobrancaId, professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Cobranca não encontrada."));

        // Step 2: Lock do plano primeiro (ordem de aplicação: Plano → Aula → Cobrança).
        planoRepository.findByIdAndProfessorIdForUpdate(cobrancaInicial.getPlanoId(), professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Plano não encontrado."));

        // Step 3 (Aula): tentativa de lock explícito no banco entre Plano e Cobrança quando aplicável
        // (integração mínima sem dependência Java do módulo Agenda). Revalida vínculo após locks.
        bloquearAulaSeAplicavel(cobrancaInicial.getAulaId(), cobrancaInicial.getAlunoId(), professor.getId());

        // Step 4: Lock da cobrança.
        Cobranca cobranca = cobrancaRepository.findByIdAndProfessorIdForUpdate(cobrancaId, professor.getId())
                .orElseThrow(() -> Exceptions.naoEncontrado("Cobranca não encontrada."));
        if (!java.util.Objects.equals(cobranca.getAulaId(), cobrancaInicial.getAulaId())) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "A cobrança foi alterada durante a operação.");
        }

        // Step 4: Revalidate after locks
        if (cobranca.getStatus() == CobrancaStatus.PAGO) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "Uma cobrança paga não pode ser cancelada.");
        }
        if (cobranca.getStatus() == CobrancaStatus.CANCELADO) {
            throw Exceptions.conflito(ErrorCodes.CONFLICT, "Uma cobrança cancelada é imutável.");
        }

        cobranca.cancelar();
        cobrancaRepository.save(cobranca);
    }

    @Override
    @Transactional
    public void gerarCobrancasMensais() {
        LocalDate hoje = LocalDate.now(clock.withZone(ZONA_CALENDARIO));
        List<PlanoPagamento> planosAtivos = planoRepository.findByPeriodicidadeAndEncerradoEmIsNull(PeriodicidadePlano.MENSAL);

        for (PlanoPagamento planoResumo : planosAtivos) {
            gerarCobrancasMensaisParaPlano(planoResumo.getId(), planoResumo.getProfessorId(), hoje);
        }
    }

    private void gerarCobrancasMensaisParaPlano(UUID planoId, UUID professorId, LocalDate hoje) {
        // Lock plano first (order: Plano → Aula → Cobrança)
        PlanoPagamento plano = planoRepository.findByIdAndProfessorIdForUpdate(planoId, professorId).orElse(null);
        if (plano == null || plano.getEncerradoEm() != null || plano.getPeriodicidade() != PeriodicidadePlano.MENSAL) {
            return;
        }

        // Calculate all missing billing cycles up to today, anchored on original plan start date
        LocalDate anchor = plano.getInicioEm();
        int cycleNumber = 0;
        while (true) {
            LocalDate cicloInicio = anchor.plusMonths(cycleNumber);
            if (cicloInicio.isAfter(hoje)) {
                break;
            }

            LocalDate proximoCiclo = anchor.plusMonths(cycleNumber + 1);
            LocalDate cicloFim = proximoCiclo.minusDays(1);

            // Inserção atômica e idempotente: evita abortar a transação em conflito de unicidade esperado.
            // A verificação prévia é otimização; o ON CONFLICT garante segurança sob concorrência.
            if (!cobrancaRepository.existsByPlanoIdAndPeriodoInicio(plano.getId(), cicloInicio)) {
                LocalDate vencimento = calcularVencimentoMensal(plano, cicloInicio);
                cobrancaRepository.inserirMensalSeNaoExistir(
                        UuidV7.generate(),
                        plano.getProfessorId(),
                        plano.getAlunoId(),
                        plano.getId(),
                        plano.getValor(),
                        vencimento,
                        cicloInicio,
                        cicloFim,
                        CobrancaStatus.PENDENTE.valorBanco());
            }

            cycleNumber++;
        }
    }

    private LocalDate calcularVencimentoMensal(PlanoPagamento plano, LocalDate cicloInicio) {
        int diaVencimento = plano.getDiaVencimento() != null ? plano.getDiaVencimento() : 1;
        // Adjust day for month end (e.g., Jan 31 -> Feb 28)
        int dia = Math.min(diaVencimento, cicloInicio.lengthOfMonth());
        LocalDate vencimento = LocalDate.of(cicloInicio.getYear(), cicloInicio.getMonth(), dia);
        // Ensure vencimento is not before plan start
        if (vencimento.isBefore(plano.getInicioEm())) {
            vencimento = plano.getInicioEm();
        }
        return vencimento;
    }

    @Override
    @Transactional
    public void gerarCobrancaPorAula(UUID professorId, UUID alunoId, UUID aulaId, LocalDate dataAula) {
        // Ordem na aplicação: Plano → Aula (quando aplicável) → Cobrança, com integração mínima via lock de linha no banco.
        // Sem serviço de domínio Agenda, a revalidação completa de elegibilidade/estado da aula na aplicação é limitada;
        // validar_e_proteger_pagamento (INSERT) continua como garantia definitiva de consistência.
        PlanoPagamento plano = planoRepository.findByAlunoIdAndProfessorIdAndEncerradoEmIsNullForUpdate(alunoId, professorId)
                .orElse(null);

        if (plano == null || plano.getPeriodicidade() != PeriodicidadePlano.POR_AULA) {
            return; // No active plan or not per-class
        }

        bloquearAulaSeAplicavel(aulaId, alunoId, professorId);

        // Não duplicar cobrança para a mesma aula (inclui cobrança cancelada/final existente).
        if (cobrancaRepository.existsByAulaId(aulaId)) {
            return;
        }

        int prazo = plano.getPrazoVencimentoDias() != null ? plano.getPrazoVencimentoDias() : 0;
        LocalDate vencimento = dataAula.plusDays(prazo);

        // Inserção atômica e idempotente: ON CONFLICT DO NOTHING evita abortar a transação em conflito de unicidade
        // esperado sob concorrência/repetição; violações de outras constraints (FK/trigger/regras) propagam.
        cobrancaRepository.inserirPorAulaSeNaoExistir(
                UuidV7.generate(),
                professorId,
                alunoId,
                plano.getId(),
                aulaId,
                plano.getValor(),
                vencimento,
                dataAula,
                dataAula,
                CobrancaStatus.PENDENTE.valorBanco());
    }

    private void bloquearAulaSeAplicavel(UUID aulaId, UUID alunoId, UUID professorId) {
        if (aulaId == null) {
            return;
        }
        // Integração mínima: lock da linha da aula para aproximar a ordem Plano → Aula → Cobrança na aplicação.
        // Sem módulo Agenda integrado, a validação completa de pertencimento/estado é delegada às triggers do banco.
        List<?> resultado = entityManager.createNativeQuery(
                        "SELECT id_aula FROM aula WHERE id_aula = :aulaId AND aluno_id = :alunoId AND professor_id = :professorId FOR UPDATE")
                .setParameter("aulaId", aulaId)
                .setParameter("alunoId", alunoId)
                .setParameter("professorId", professorId)
                .getResultList();
        if (resultado.isEmpty()) {
            throw Exceptions.naoEncontrado("Aula não encontrada.");
        }
    }

    /** Exporta cobranças pagas de um mês em CSV após confirmar a senha (RF39). */
    @Transactional(readOnly = true)
    public CobrancaCsv exportarCsv(UUID usuarioId, String mes, String senha) {
        Professor professor = professorService.obterPorUsuarioId(usuarioId);

        if (!autenticacaoService.confirmarSenha(usuarioId, senha)) {
            throw Exceptions.acessoNegado(ErrorCodes.FORBIDDEN, "Senha inválida.");
        }

        LocalDate[] periodo = mesDo(mes);
        List<Cobranca> cobrancas = cobrancaRepository.buscarPagasPorMes(
                professor.getId(), periodo[0], periodo[1]);

        // Build CSV with approved columns in exact order
        StringBuilder csv = new StringBuilder();
        // BOM UTF-8
        csv.append("\uFEFF");
        csv.append("Aluno,Valor,Vencimento,Recebimento,Período inicial,Período final,Forma de pagamento,Descrição\r\n");
        for (Cobranca c : cobrancas) {
            String nomeAluno;
            try {
                Aluno aluno = alunoService.obter(usuarioId, c.getAlunoId());
                nomeAluno = aluno.getNome();
            } catch (br.com.compasso.shared.error.ApiException e) {
                if (ErrorCodes.NOT_FOUND.equals(e.code())) {
                    // Situação esperada apenas se o vínculo estiver inconsistente; preserva isolamento sem expor UUID.
                    nomeAluno = "";
                } else {
                    throw e;
                }
            }
            csv.append(csvEscape(nomeAluno)).append(",");
            csv.append(csvEscape(c.getValor().setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString())).append(",");
            csv.append(csvEscape(c.getVencimentoEm().toString())).append(",");
            csv.append(csvEscape(c.getDataPagamento() != null ? c.getDataPagamento().toString() : "")).append(",");
            csv.append(csvEscape(c.getPeriodoInicio() != null ? c.getPeriodoInicio().toString() : "")).append(",");
            csv.append(csvEscape(c.getPeriodoFim() != null ? c.getPeriodoFim().toString() : "")).append(",");
            csv.append(csvEscape(c.getFormaPagamento() != null ? c.getFormaPagamento() : "")).append(",");
            csv.append(csvEscape(c.getDescricao() != null ? c.getDescricao() : "")).append("\r\n");
        }

        String nomeArquivo = "cobrancas-" + mes + ".csv";
        byte[] conteudo = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new CobrancaCsv(nomeArquivo, conteudo);
    }

    /** Escapes a value for CSV (RFC 4180) and neutralizes spreadsheet formulas. */
    private String csvEscape(String value) {
        if (value == null) return "";
        // Neutralize CSV/formula injection, including values with leading whitespace before dangerous prefixes.
        String trimmed = value.stripLeading();
        if (!trimmed.isEmpty()) {
            char primeiro = trimmed.charAt(0);
            if (primeiro == '=' || primeiro == '+' || primeiro == '-' || primeiro == '@' || primeiro == '\t' || primeiro == '\r') {
                value = "'" + value;
            }
        }
        // RFC 4180 escaping
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private LocalDate[] mesDo(String mes) {
        Matcher correspondencia = MES_PATTERN.matcher(mes);
        if (!correspondencia.matches()) {
            throw Exceptions.requisicaoInvalida("Informe o mês no formato YYYY-MM.", List.of(
                    new ValidationError("mes", "INVALID_FORMAT", "O mês deve estar no formato YYYY-MM.")));
        }

        LocalDate inicio = LocalDate.of(Integer.parseInt(correspondencia.group(1)), Integer.parseInt(correspondencia.group(2)), 1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);
        return new LocalDate[] {inicio, fim};
    }

    private CobrancaStatus statusDo(String status) {
        try {
            return CobrancaStatus.valueOf(status);
        } catch (IllegalArgumentException excecao) {
            throw Exceptions.requisicaoInvalida("A situação informada é inválida.", List.of(
                    new ValidationError("status", "INVALID_VALUE", "A situação deve ser PENDENTE, PAGO ou CANCELADO.")));
        }
    }

    // Conflitos de unicidade esperados na geração são tratados de forma atômica no banco
    // (INSERT ... ON CONFLICT DO NOTHING), evitando abortar a transação PostgreSQL.

    private void validarConfiguracao(PeriodicidadePlano periodicidade,
            java.math.BigDecimal valor,
            LocalDate inicioEm,
            Integer diaVencimento,
            Integer prazoVencimentoDias) {
        if (periodicidade == PeriodicidadePlano.MENSAL) {
            if (diaVencimento == null || diaVencimento < 1 || diaVencimento > 28) {
                throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", List.of(
                        new ValidationError("diaVencimento", "INVALID_VALUE",
                                "Para periodicidade mensal o dia de vencimento deve ser informado entre 1 e 28.")));
            }
            if (prazoVencimentoDias != null) {
                throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", List.of(
                        new ValidationError("prazoVencimentoDias", "INVALID_VALUE",
                                "Para periodicidade mensal o prazo de vencimento não deve ser informado.")));
            }
        }
        if (periodicidade == PeriodicidadePlano.POR_AULA) {
            if (prazoVencimentoDias == null) {
                throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", List.of(
                        new ValidationError("prazoVencimentoDias", "REQUIRED",
                                "Para periodicidade por aula o prazo de vencimento é obrigatório.")));
            }
            if (diaVencimento != null) {
                throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", List.of(
                        new ValidationError("diaVencimento", "INVALID_VALUE",
                                "Para periodicidade por aula o dia de vencimento não deve ser informado.")));
            }
        }
    }
}
