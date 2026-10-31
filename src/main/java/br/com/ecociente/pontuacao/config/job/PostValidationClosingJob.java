package br.com.ecociente.pontuacao.config.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import br.com.ecociente.pontuacao.core.gateway.PostagemValidacaoGateway;
import br.com.ecociente.pontuacao.core.service.PostagemValidacaoService;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class PostValidationClosingJob {

    private static final Logger log = LoggerFactory.getLogger(PostValidationClosingJob.class);

    private final PostagemValidacaoGateway gateway;
    private final PostagemValidacaoService service;
    private final int batchSize;

    private int ultimoId = 0;

    public PostValidationClosingJob(
            PostagemValidacaoGateway gateway,
            PostagemValidacaoService service,
            @Value("${app.validation.batch-size:100}") int batchSize) {
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException(
                    "O tamanho do lote deve estar entre 1 e 1000");
        }

        this.gateway = gateway;
        this.service = service;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.validation.delay-ms:60000}")
    public synchronized void executar() {
        try {
            var postagens = gateway.buscarVencidasApos(
                    ultimoId,
                    batchSize);

            if (postagens.isEmpty()) {
                ultimoId = 0;
                return;
            }

            for (Integer postagemId : postagens) {
                try {
                    service.encerrar(postagemId);
                } catch (RuntimeException exception) {
                    log.error(
                            "Falha ao encerrar postagemId={}",
                            postagemId,
                            exception);
                } finally {
                    ultimoId = postagemId;
                }
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Falha ao consultar lote de postagens vencidas ultimoId={}",
                    ultimoId,
                    exception);
        }
    }
}