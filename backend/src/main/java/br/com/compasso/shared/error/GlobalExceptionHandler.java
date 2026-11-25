package br.com.compasso.shared.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/** Tratamento centralizado de erros no formato Problem Details. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> tratarApiException(ApiException ex, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetailFactory.criar(request, ex.status(), ex.code(),
                ProblemDetailFactory.tituloDe(ex.status()), ex.getMessage(), ex.errors());

        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(ex.status());
        if (ex instanceof LimiteExcedidoException limite) {
            resposta.header(HttpHeaders.RETRY_AFTER, Long.toString(limite.segundosDeEspera()));
        }

        return resposta.body(problema);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> tratarValidacao(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ValidationError> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(this::paraErroDeCampo)
                .toList();

        return respostaValidacao(request, erros);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> tratarValidacaoDeParametros(HandlerMethodValidationException ex,
            HttpServletRequest request) {
        return respostaValidacao(request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> tratarCorpoIlegivel(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        ProblemDetail problema = ProblemDetailFactory.criar(request, HttpStatus.BAD_REQUEST,
                ErrorCodes.MALFORMED_JSON, ProblemDetailFactory.tituloDe(HttpStatus.BAD_REQUEST),
                "O corpo da requisição é inválido ou contém propriedades desconhecidas.", List.of());

        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ProblemDetail> tratarParametroInvalido(Exception ex, HttpServletRequest request) {
        return respostaValidacao(request, List.of());
    }

    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<ProblemDetail> tratarRecursoInexistente(Exception ex, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetailFactory.criar(request, HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND,
                ProblemDetailFactory.tituloDe(HttpStatus.NOT_FOUND), "O recurso solicitado não existe.", List.of());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problema);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        ProblemDetail problema = ProblemDetailFactory.criar(request, HttpStatus.METHOD_NOT_ALLOWED,
                ErrorCodes.METHOD_NOT_ALLOWED, ProblemDetailFactory.tituloDe(HttpStatus.METHOD_NOT_ALLOWED),
                "O método HTTP não é suportado por este recurso.", List.of());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problema);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> tratarAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetailFactory.criar(request, HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
                ProblemDetailFactory.tituloDe(HttpStatus.FORBIDDEN), "A operação não é permitida.", List.of());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problema);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> tratarViolacaoDeIntegridade(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Violação de integridade ao processar {} {}", request.getMethod(), request.getRequestURI());

        ProblemDetail problema = ProblemDetailFactory.criar(request, HttpStatus.CONFLICT, ErrorCodes.CONFLICT,
                ProblemDetailFactory.tituloDe(HttpStatus.CONFLICT),
                "A operação conflita com o estado atual dos dados.", List.of());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {} {}", request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail problema = ProblemDetailFactory.criar(request, HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCodes.INTERNAL_ERROR, ProblemDetailFactory.tituloDe(HttpStatus.INTERNAL_SERVER_ERROR),
                "Não foi possível concluir a operação.", List.of());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema);
    }

    private ResponseEntity<ProblemDetail> respostaValidacao(HttpServletRequest request, List<ValidationError> erros) {
        ProblemDetail problema = ProblemDetailFactory.criar(request, HttpStatus.BAD_REQUEST,
                ErrorCodes.VALIDATION_ERROR, ProblemDetailFactory.tituloDe(HttpStatus.BAD_REQUEST),
                "Um ou mais campos são inválidos.", erros);

        return ResponseEntity.badRequest().body(problema);
    }

    private ValidationError paraErroDeCampo(FieldError erro) {
        return new ValidationError(erro.getField(), codigoDe(erro), mensagemDe(erro));
    }

    private String codigoDe(FieldError erro) {
        if ("Email".equals(erro.getCode()) || "Pattern".equals(erro.getCode())) {
            return "INVALID_FORMAT";
        }
        if ("NotNull".equals(erro.getCode()) || "NotBlank".equals(erro.getCode())
                || "NotEmpty".equals(erro.getCode())) {
            return "REQUIRED";
        }
        if ("Size".equals(erro.getCode()) || "Length".equals(erro.getCode())) {
            return "INVALID_LENGTH";
        }
        if ("Min".equals(erro.getCode()) || "Max".equals(erro.getCode())
                || "DecimalMin".equals(erro.getCode()) || "DecimalMax".equals(erro.getCode())) {
            return "OUT_OF_RANGE";
        }
        return "INVALID_VALUE";
    }

    private String mensagemDe(FieldError erro) {
        return erro.getDefaultMessage() == null ? "Valor inválido." : erro.getDefaultMessage();
    }
}
