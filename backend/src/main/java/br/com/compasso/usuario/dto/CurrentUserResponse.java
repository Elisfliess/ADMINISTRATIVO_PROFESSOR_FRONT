package br.com.compasso.usuario.dto;

import java.util.UUID;

public record CurrentUserResponse(UUID id, String email, boolean emailVerificado, boolean termosAceitos) {
}
