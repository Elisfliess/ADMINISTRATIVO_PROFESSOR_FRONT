package br.com.compasso.aluno.dto;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.Size;

/**
 * Trecho de {@code responsavelLegal} em JSON Merge Patch (RFC 7396).
 *
 * <p>Registra a presença de cada campo para que a atualização parcial preserve
 * os valores omitidos. {@code null} explícito em um campo é tratado como
 * remoção e, quando resultar em responsável incompleto, é rejeitado pelo
 * Service conforme RN16.</p>
 */
public class ResponsavelLegalPatchRequest {

    @Size(max = 150, message = "O nome do responsável legal deve ter no máximo 150 caracteres.")
    private String nome;

    @Size(max = 30, message = "O contato do responsável legal deve ter no máximo 30 caracteres.")
    private String contato;

    private final Set<String> camposPresentes = new HashSet<>();

    public boolean possui(String campo) {
        return camposPresentes.contains(campo);
    }

    /** Indica se o objeto trouxe ao menos um campo, diferenciando de {@code {}}. */
    public boolean possuiDados() {
        return !camposPresentes.isEmpty();
    }

    @JsonSetter("nome")
    public void setNome(String nome) {
        this.nome = nome;
        camposPresentes.add("nome");
    }

    @JsonSetter("contato")
    public void setContato(String contato) {
        this.contato = contato;
        camposPresentes.add("contato");
    }

    public String getNome() {
        return nome;
    }

    public String getContato() {
        return contato;
    }
}
