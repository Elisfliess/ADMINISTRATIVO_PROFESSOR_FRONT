package br.com.compasso.usuario.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.compasso.usuario.entity.TipoDocumentoLegal;
import br.com.compasso.usuario.entity.VersaoDocumentoLegal;

public interface VersaoDocumentoLegalRepository extends JpaRepository<VersaoDocumentoLegal, UUID> {

    Optional<VersaoDocumentoLegal> findFirstByTipoOrderByPublicadaEmDesc(TipoDocumentoLegal tipo);

    Optional<VersaoDocumentoLegal> findByTipoAndVersao(TipoDocumentoLegal tipo, String versao);
}
