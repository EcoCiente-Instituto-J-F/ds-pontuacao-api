package br.com.ecociente.pontuacao.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;

@Service
public class RedisAdminService {

    private static final Logger log =
        LoggerFactory.getLogger(RedisAdminService.class);

    private final SincronizacaoPontuacaoGateway gateway;
    private final SincronizacaoPontuacaoService service;
    private final StringRedisTemplate redisTemplate;
    private final int batchSize;

    public RedisAdminService(
    SincronizacaoPontuacaoGateway gateway,
    SincronizacaoPontuacaoService service,
    StringRedisTemplate redisTemplate,
    @Value("${app.ranking.sync-batch-size:100}") int batchSize
) {
    if (batchSize < 1 || batchSize > 1000) {
        throw new IllegalArgumentException(
            "O lote de sincronização deve estar entre 1 e 1000"
        );
    }

    this.gateway = gateway;
    this.service = service;
    this.redisTemplate = redisTemplate;
    this.batchSize = batchSize;
}

    public ResultadoSincronizacao sincronizarPendentes() {
        var pendentes = gateway.buscarIdsPendentes(batchSize);

        int sincronizadas = 0;
        int falhas = 0;

        for (Long movimentacaoId : pendentes) {
            try {
                if (service.sincronizar(movimentacaoId)) {
                    sincronizadas++;
                }
            } catch (RuntimeException exception) {
                falhas++;

                log.error(
                    "Falha na sincronização manual movimentacaoId={}",
                    movimentacaoId,
                    exception
                );

                registrarFalha(movimentacaoId);
            }
        }

        return new ResultadoSincronizacao(
            pendentes.size(),
            sincronizadas,
            falhas,
            gateway.contarPendentes()
        );
    }

    public DiagnosticoRedis consultarStatus() {
        boolean disponivel;

        try {
            Boolean resposta = redisTemplate.execute(
                (RedisCallback<Boolean>) connection ->
                    "PONG".equalsIgnoreCase(connection.ping())
            );

            disponivel = Boolean.TRUE.equals(resposta);
        } catch (RuntimeException exception) {
            disponivel = false;

            log.warn(
                "Redis indisponível durante consulta de diagnóstico",
                exception
            );
        }

        return new DiagnosticoRedis(
            disponivel,
            gateway.contarPendentes()
        );
    }

    private void registrarFalha(Long movimentacaoId) {
        try {
            gateway.registrarFalha(movimentacaoId);
        } catch (RuntimeException exception) {
            log.error(
                "Falha ao registrar tentativa movimentacaoId={}",
                movimentacaoId,
                exception
            );
        }
    }

    public record ResultadoSincronizacao(
        int processadas,
        int sincronizadas,
        int falhas,
        Long pendentesRestantes
    ) {
    }

    public record DiagnosticoRedis(
        boolean redisDisponivel,
        Long movimentacoesPendentes
    ) {
    }
}