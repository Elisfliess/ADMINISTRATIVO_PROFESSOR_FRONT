package br.com.compasso.usuario.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.usuario.entity.AceiteDocumentoLegal;
import br.com.compasso.usuario.entity.TipoDocumentoLegal;
import br.com.compasso.usuario.entity.VersaoDocumentoLegal;
import br.com.compasso.usuario.repository.AceiteDocumentoLegalRepository;
import br.com.compasso.usuario.repository.VersaoDocumentoLegalRepository;

/**
 * Consulta as versões vigentes dos documentos legais e registra o aceite do
 * usuário. O instante do aceite é sempre gerado pelo servidor (RF35).
 */
@Service
public class LegalDocumentService {

    private final VersaoDocumentoLegalRepository versaoRepository;
    private final AceiteDocumentoLegalRepository aceiteRepository;
    private final Clock clock;

    public LegalDocumentService(VersaoDocumentoLegalRepository versaoRepository,
            AceiteDocumentoLegalRepository aceiteRepository, Clock clock) {
        this.versaoRepository = versaoRepository;
        this.aceiteRepository = aceiteRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean termosAceitos(UUID usuarioId) {
        VersaoDocumentoLegal termos = versaoVigente(TipoDocumentoLegal.TERMOS_USO);
        VersaoDocumentoLegal politica = versaoVigente(TipoDocumentoLegal.POLITICA_PRIVACIDADE);

        long aceites = aceiteRepository.countByUsuarioIdAndVersaoIdIn(usuarioId,
                List.of(termos.getId(), politica.getId()));

        return aceites == 2;
    }

    @Transactional
    public void registrarAceite(UUID usuarioId, String termosVersao, String politicaVersao) {
        VersaoDocumentoLegal termos = versaoVigente(TipoDocumentoLegal.TERMOS_USO);
        VersaoDocumentoLegal politica = versaoVigente(TipoDocumentoLegal.POLITICA_PRIVACIDADE);

        if (!termos.getVersao().equals(termosVersao) || !politica.getVersao().equals(politicaVersao)) {
            throw versaoNaoEncontrada();
        }

        Instant agora = clock.instant();
        registrarSeNecessario(usuarioId, termos.getId(), agora);
        registrarSeNecessario(usuarioId, politica.getId(), agora);
    }

    private void registrarSeNecessario(UUID usuarioId, UUID versaoId, Instant agora) {
        if (!aceiteRepository.existsByUsuarioIdAndVersaoId(usuarioId, versaoId)) {
            aceiteRepository.save(new AceiteDocumentoLegal(usuarioId, versaoId, agora));
        }
    }

    private VersaoDocumentoLegal versaoVigente(TipoDocumentoLegal tipo) {
        return versaoRepository.findFirstByTipoOrderByPublicadaEmDesc(tipo)
                .orElseThrow(LegalDocumentService::versaoNaoEncontrada);
    }

    private static br.com.compasso.shared.error.ApiException versaoNaoEncontrada() {
        return Exceptions.naoEncontrado(ErrorCodes.LEGAL_VERSION_NOT_FOUND,
                "A versão de documento legal informada não está vigente.");
    }
}
