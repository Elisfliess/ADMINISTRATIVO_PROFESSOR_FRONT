package br.com.compasso.aluno.dto;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Documento JSON Merge Patch (RFC 7396) de aluno.
 *
 * <p>O contrato distingue propriedade ausente de propriedade presente com
 * {@code null}. Por isso cada setter registra a presença do campo: ausente
 * mantém o valor atual; presente com {@code null} limpa o campo, quando
 * permitido. Campos obrigatórios (nome, telefone e data de nascimento) não
 * aceitam {@code null} e são validados pelo Service após a mesclagem. O objeto
 * {@code responsavelLegal} também é mesclado campo a campo: propriedades
 * omitidas preservam os valores atuais.</p>
 */
public class AlunoPatchRequest {

    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String nome;

    @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.")
    private String telefone;

    @PastOrPresent(message = "A data de nascimento não pode estar no futuro.")
    private LocalDate dataNascimento;

    @Email(message = "Informe um endereço de e-mail válido.")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
    private String email;

    private String observacoes;

    @Valid
    private ResponsavelLegalPatchRequest responsavelLegal;

    private final Set<String> camposPresentes = new HashSet<>();

    public boolean possui(String campo) {
        return camposPresentes.contains(campo);
    }

    @JsonSetter("nome")
    public void setNome(String nome) {
        this.nome = nome;
        camposPresentes.add("nome");
    }

    @JsonSetter("telefone")
    public void setTelefone(String telefone) {
        this.telefone = telefone;
        camposPresentes.add("telefone");
    }

    @JsonSetter("dataNascimento")
    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
        camposPresentes.add("dataNascimento");
    }

    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = email;
        camposPresentes.add("email");
    }

    @JsonSetter("observacoes")
    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
        camposPresentes.add("observacoes");
    }

    @JsonSetter("responsavelLegal")
    public void setResponsavelLegal(ResponsavelLegalPatchRequest responsavelLegal) {
        this.responsavelLegal = responsavelLegal;
        camposPresentes.add("responsavelLegal");
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getEmail() {
        return email;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public ResponsavelLegalPatchRequest getResponsavelLegal() {
        return responsavelLegal;
    }
}
