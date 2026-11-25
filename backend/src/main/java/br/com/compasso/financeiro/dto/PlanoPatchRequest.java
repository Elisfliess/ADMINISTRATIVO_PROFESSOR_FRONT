package br.com.compasso.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Documento JSON Merge Patch (RFC 7396) de plano de pagamento.
 *
 * <p>Permite alterar periodicidade, valor, início, dia de vencimento e prazo de
 * vencimento somente enquanto o plano estiver ativo e sem cobranças geradas.</p>
 *
 * <p>Semântica de presença: propriedade ausente preserva o valor atual; propriedade
 * presente participa da atualização. Campos que não admitem limpeza ({@code periodicidade},
 * {@code valor}, {@code inicioEm}) rejeitam {@code null}. Campos dependentes da
 * periodicidade ({@code diaVencimento}, {@code prazoVencimentoDias}) aceitam {@code null}
 * para limpeza quando compatível com o estado final validado (ex.: troca de periodicidade).</p>
 */
public class PlanoPatchRequest {

    @Pattern(regexp = "MENSAL|POR_AULA", message = "A periodicidade deve ser MENSAL ou POR_AULA.")
    private String periodicidade;

    @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
    @jakarta.validation.constraints.Digits(integer = 8, fraction = 2, message = "O valor deve ter no máximo 8 dígitos inteiros e 2 decimais.")
    private BigDecimal valor;

    private LocalDate inicioEm;

    @Min(value = 1, message = "O dia de vencimento deve estar entre 1 e 28.")
    @Max(value = 28, message = "O dia de vencimento deve estar entre 1 e 28.")
    private Integer diaVencimento;

    @Min(value = 0, message = "O prazo de vencimento deve ser maior ou igual a zero.")
    private Integer prazoVencimentoDias;

    private final Set<String> camposPresentes = new HashSet<>();

    public boolean possui(String campo) {
        return camposPresentes.contains(campo);
    }

    @JsonSetter("periodicidade")
    public void setPeriodicidade(String periodicidade) {
        this.periodicidade = periodicidade;
        camposPresentes.add("periodicidade");
    }

    @JsonSetter("valor")
    public void setValor(BigDecimal valor) {
        this.valor = valor;
        camposPresentes.add("valor");
    }

    @JsonSetter("inicioEm")
    public void setInicioEm(LocalDate inicioEm) {
        this.inicioEm = inicioEm;
        camposPresentes.add("inicioEm");
    }

    @JsonSetter("diaVencimento")
    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
        camposPresentes.add("diaVencimento");
    }

    @JsonSetter("prazoVencimentoDias")
    public void setPrazoVencimentoDias(Integer prazoVencimentoDias) {
        this.prazoVencimentoDias = prazoVencimentoDias;
        camposPresentes.add("prazoVencimentoDias");
    }

    public String getPeriodicidade() {
        return periodicidade;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getInicioEm() {
        return inicioEm;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public Integer getPrazoVencimentoDias() {
        return prazoVencimentoDias;
    }
}
