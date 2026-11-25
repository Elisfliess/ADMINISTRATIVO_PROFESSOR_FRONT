package br.com.compasso.financeiro.scheduler;

import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.compasso.financeiro.service.GeracaoCobrancaService;

/**
 * Agendador diário para geração de cobranças mensais.
 * Delega a lógica de negócio para a camada de aplicação.
 */
@Component
public class GeracaoCobrancaAgendador {

    private static final Logger log = LoggerFactory.getLogger(GeracaoCobrancaAgendador.class);
    private static final ZoneId ZONA_CALENDARIO = ZoneId.of("America/Sao_Paulo");

    private final GeracaoCobrancaService geracaoService;

    public GeracaoCobrancaAgendador(GeracaoCobrancaService geracaoService) {
        this.geracaoService = geracaoService;
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Sao_Paulo")
    public void gerarCobrancasMensais() {
        log.info("Iniciando geração mensal de cobranças.");
        geracaoService.gerarCobrancasMensais();
        log.info("Geração mensal de cobranças concluída.");
    }
}