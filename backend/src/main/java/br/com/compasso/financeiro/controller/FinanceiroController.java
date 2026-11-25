package br.com.compasso.financeiro.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.compasso.financeiro.dto.CobrancaBaixaRequest;
import br.com.compasso.financeiro.dto.CobrancaCsv;
import br.com.compasso.financeiro.dto.CobrancaExportacaoRequest;
import br.com.compasso.financeiro.dto.CobrancaResponse;
import br.com.compasso.financeiro.dto.PlanoCreateRequest;
import br.com.compasso.financeiro.dto.PlanoPatchRequest;
import br.com.compasso.financeiro.dto.PlanoResponse;
import br.com.compasso.financeiro.service.FinanceiroService;
import br.com.compasso.shared.web.PageResponse;
import br.com.compasso.usuario.security.UsuarioAutenticado;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    public FinanceiroController(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    @GetMapping("/alunos/{alunoId}/planos")
    public List<PlanoResponse> listarPlanos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID alunoId) {

        return financeiroService.listar(usuario.id(), alunoId);
    }

    @PostMapping("/alunos/{alunoId}/planos")
    public ResponseEntity<PlanoResponse> criarPlano(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID alunoId,
            @Valid @RequestBody PlanoCreateRequest requisicao) {

        PlanoResponse resposta = financeiroService.criar(usuario.id(), alunoId, requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping("/planos/{planoId}")
    public PlanoResponse obterPlano(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID planoId) {

        return financeiroService.obter(usuario.id(), planoId);
    }

    @PatchMapping(value = "/planos/{planoId}", consumes = "application/merge-patch+json")
    public PlanoResponse atualizarPlano(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID planoId,
            @Valid @RequestBody PlanoPatchRequest patch) {

        return financeiroService.atualizar(usuario.id(), planoId, patch);
    }

    @PostMapping("/planos/{planoId}/encerramento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void encerrarPlano(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID planoId) {

        financeiroService.encerrar(usuario.id(), planoId);
    }

    @GetMapping("/cobrancas")
    public PageResponse<CobrancaResponse> listarCobrancas(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID alunoId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return financeiroService.listarCobrancas(usuario.id(), mes, status, alunoId, pageable);
    }

    @GetMapping("/cobrancas/{cobrancaId}")
    public CobrancaResponse obterCobranca(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID cobrancaId) {

        return financeiroService.obterCobranca(usuario.id(), cobrancaId);
    }

    @PostMapping("/cobrancas/{cobrancaId}/baixa")
    public CobrancaResponse baixarCobranca(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID cobrancaId,
            @Valid @RequestBody CobrancaBaixaRequest requisicao) {

        return financeiroService.baixar(usuario.id(), cobrancaId, requisicao);
    }

    @PostMapping("/cobrancas/{cobrancaId}/cancelamento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarCobranca(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID cobrancaId) {

        financeiroService.cancelar(usuario.id(), cobrancaId);
    }

    @PostMapping(value = "/cobrancas/exportacao", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "text/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CobrancaExportacaoRequest requisicao) {

        CobrancaCsv arquivo = financeiroService.exportarCsv(usuario.id(), requisicao.mes(), requisicao.senha());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nomeArquivo()).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(arquivo.conteudo());
    }
}
