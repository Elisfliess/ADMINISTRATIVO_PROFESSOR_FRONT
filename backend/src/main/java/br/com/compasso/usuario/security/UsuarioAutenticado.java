package br.com.compasso.usuario.security;

import java.security.Principal;
import java.util.UUID;

/** Identidade resolvida da sessão autenticada. */
public record UsuarioAutenticado(UUID id, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
