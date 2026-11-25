package br.com.compasso.usuario.repository;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.compasso.usuario.entity.AceiteDocumentoLegal;

public interface AceiteDocumentoLegalRepository extends JpaRepository<AceiteDocumentoLegal, UUID> {

    boolean existsByUsuarioIdAndVersaoId(UUID usuarioId, UUID versaoId);

    long countByUsuarioIdAndVersaoIdIn(UUID usuarioId, Collection<UUID> versaoIds);
}
