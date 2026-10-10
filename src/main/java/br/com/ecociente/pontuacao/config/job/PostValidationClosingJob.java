
package br.com.ecociente.pontuacao.config.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.gateway.PostagemValidacaoGateway;
import br.com.ecociente.pontuacao.core.service.PostagemValidacaoService;

@Component
public class PostValidationClosingJob {

    private static final Logger log = LoggerFactory.getLogger(PostValidationClosingJob.class);

    private final PostagemValidacaoGateway gateway;
    private final PostagemValidacaoService service;
    private final int batchSize;

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
    public void executar() {

        var postagens = gateway.buscarVencidas(batchSize);

        for (Integer postagemId : postagens) {
            try {
                service.encerrar(postagemId);
            } catch (RuntimeException ex) {
                log.error(
                        "Falha ao encerrar postagemId={}",
                        postagemId,
                        ex);
            }
        }
    }
}
