
package br.com.ecociente.pontuacao.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;

import lombok.RequiredArgsConstructor;

@Service
public class RedisAdminService {

  private final SincronizacaoPontuacaoGateway gateway;
  private final SincronizacaoPontuacaoService service;
  private final StringRedisTemplate redisTemplate;
  private final int batchSize;

  public RedisAdminService(
      SincronizacaoPontuacaoGateway gateway,
      SincronizacaoPontuacaoService service,
      StringRedisTemplate redisTemplate,
      @Value("${app.ranking.sync-batch-size:100}") int batchSize) {

    this.gateway = gateway;
    this.service = service;
    this.redisTemplate = redisTemplate;
    this.batchSize = batchSize;
  }

  public ResultadoSincronizacao sincronizarPendentes() {

    var pendentes = gateway.buscarIdsPendentes(batchSize);

    int sincronizadas = 0;
    int falhas = 0;

    for (Long id : pendentes) {
      try {
        if (service.sincronizar(id)) {
          sincronizadas++;
        }
      } catch (RuntimeException ex) {
        falhas++;
        gateway.registrarFalha(id);
      }
    }

    return new ResultadoSincronizacao(
        pendentes.size(),
        sincronizadas,
        falhas,
        gateway.contarPendentes());
  }

  public DiagnosticoRedis consultarStatus() {

    Boolean conectado;

    try {
      conectado = redisTemplate.execute(
          (RedisCallback<Boolean>) connection -> "PONG".equalsIgnoreCase(connection.ping()));
    } catch (RuntimeException ex) {
      conectado = false;
    }

    return new DiagnosticoRedis(
        Boolean.TRUE.equals(conectado),
        gateway.contarPendentes());
  }

  public record ResultadoSincronizacao(
      int processadas,
      int sincronizadas,
      int falhas,
      Long pendentesRestantes) {
  }

  public record DiagnosticoRedis(
      boolean redisDisponivel,
      Long movimentacoesPendentes) {
  }
}
