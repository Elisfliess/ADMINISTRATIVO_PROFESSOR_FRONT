package br.com.compasso.aluno.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.compasso.aluno.dto.AlunoAnonimizacaoPreviaResponse;
import br.com.compasso.aluno.dto.AlunoAnonimizacaoRequest;
import br.com.compasso.aluno.dto.AlunoCreateRequest;
import br.com.compasso.aluno.dto.AlunoCsv;
import br.com.compasso.aluno.dto.AlunoExportacaoRequest;
import br.com.compasso.aluno.dto.AlunoPatchRequest;
import br.com.compasso.aluno.dto.AlunoResponse;
import br.com.compasso.aluno.entity.Aluno;
import br.com.compasso.aluno.service.AlunoService;
import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.shared.error.ValidationError;
import br.com.compasso.shared.web.PageResponse;
import br.com.compasso.usuario.security.UsuarioAutenticado;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/alunos")
public class AlunoController {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of("id", "nome", "ativo");

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @GetMapping
    public PageResponse<AlunoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo) {

        Page<Aluno> pagina = alunoService.listar(usuario.id(), nome, ativo, paginacaoPermitida(pageable));
        return PageResponse.de(pagina, AlunoResponse::de);
    }

    @PostMapping
    public ResponseEntity<AlunoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AlunoCreateRequest requisicao) {

        Aluno aluno = alunoService.criar(usuario.id(), requisicao);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{alunoId}")
                .buildAndExpand(aluno.getId())
                .toUri();

        return ResponseEntity.created(localizacao).body(AlunoResponse.de(aluno));
    }

    @GetMapping("/{alunoId}")
    public AlunoResponse obter(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID alunoId) {

        return AlunoResponse.de(alunoService.obter(usuario.id(), alunoId));
    }

    @PatchMapping(value = "/{alunoId}", consumes = "application/merge-patch+json")
    public AlunoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID alunoId,
            @Valid @RequestBody AlunoPatchRequest requisicao) {

        return AlunoResponse.de(alunoService.atualizar(usuario.id(), alunoId, requisicao));
    }

    @PostMapping("/{alunoId}/reativacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID alunoId) {

        alunoService.reativar(usuario.id(), alunoId);
    }

    @PostMapping(value = "/{alunoId}/exportacao", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "text/csv")
    public ResponseEntity<byte[]> exportar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID alunoId,
            @Valid @RequestBody AlunoExportacaoRequest requisicao) {

        AlunoCsv arquivo = alunoService.exportarCsv(usuario.id(), alunoId, requisicao.senha());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nomeArquivo()).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(arquivo.conteudo());
    }

    @GetMapping("/{alunoId}/anonimizacao/previa")
    public AlunoAnonimizacaoPreviaResponse previaAnonimizacao(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID alunoId) {

        return alunoService.previaAnonimizacao(usuario.id(), alunoId);
    }

    @PostMapping("/{alunoId}/anonimizacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonimizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID alunoId,
            @Valid @RequestBody AlunoAnonimizacaoRequest requisicao) {

        alunoService.anonimizar(usuario.id(), alunoId, requisicao);
    }

    /**
     * Aplica o padrão estável do contrato: apenas campos conhecidos podem ser
     * usados na ordenação e o UUID é critério secundário.
     */
    private Pageable paginacaoPermitida(Pageable pageable) {
        for (Sort.Order ordem : pageable.getSort()) {
            if (!CAMPOS_ORDENACAO.contains(ordem.getProperty())) {
                throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", List.of(
                        new ValidationError("sort", "INVALID_VALUE",
                                "A ordenação permitida é por id, nome ou ativo.")));
            }
        }

        Sort ordenacao = pageable.getSort();
        if (ordenacao.stream().noneMatch(ordem -> ordem.getProperty().equals("id"))) {
            ordenacao = ordenacao.and(Sort.by(Sort.Order.asc("id")));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordenacao);
    }
}
