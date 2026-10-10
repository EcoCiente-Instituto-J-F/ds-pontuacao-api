package br.com.ecociente.pontuacao.config.job;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;
import br.com.ecociente.pontuacao.core.service.SincronizacaoPontuacaoService;

@Component
public class RedisPendingSyncJob {

    private static final Logger log = LoggerFactory.getLogger(RedisPendingSyncJob.class);

    private final SincronizacaoPontuacaoGateway gateway;
    private final SincronizacaoPontuacaoService service;
    private final Clock clock;
    private final int batchSize;

    private Instant proximaTentativa = Instant.MIN;
    private long esperaSegundos = 5;

    public RedisPendingSyncJob(
            SincronizacaoPontuacaoGateway gateway,
            SincronizacaoPontuacaoService service,
            Clock clock,
            @Value("${app.ranking.sync-batch-size:100}") int batchSize) {
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException(
                    "O lote de sincronização deve estar entre 1 e 1000");
        }

        this.gateway = gateway;
        this.service = service;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.ranking.sync-delay-ms:5000}")
    public void executar() {
        if (clock.instant().isBefore(proximaTentativa)) {
            return;
        }

        boolean houveFalha = false;
        int sincronizadas = 0;

        try {
            var pendentes = gateway.buscarIdsPendentes(batchSize);

            for (Long movimentacaoId : pendentes) {
                try {
                    if (service.sincronizar(movimentacaoId)) {
                        sincronizadas++;
                    }
                } catch (RuntimeException ex) {
                    houveFalha = true;

                    log.error(
                            "Falha ao sincronizar movimentacaoId={}",
                            movimentacaoId,
                            ex);

                    try {
                        gateway.registrarFalha(movimentacaoId);
                    } catch (RuntimeException registroEx) {
                        log.error(
                                "Falha ao registrar tentativa movimentacaoId={}",
                                movimentacaoId,
                                registroEx);
                    }
                }
            }
        } catch (RuntimeException ex) {
            houveFalha = true;
            log.error("Falha ao consultar movimentações pendentes", ex);
        }

        if (sincronizadas > 0) {
            log.info(
                    "Sincronização concluída quantidade={}",
                    sincronizadas);
        }

        if (houveFalha) {
            proximaTentativa = clock.instant().plusSeconds(esperaSegundos);
            esperaSegundos = Math.min(esperaSegundos * 2, 60);
        } else {
            proximaTentativa = Instant.MIN;
            esperaSegundos = 5;
        }
    }
}