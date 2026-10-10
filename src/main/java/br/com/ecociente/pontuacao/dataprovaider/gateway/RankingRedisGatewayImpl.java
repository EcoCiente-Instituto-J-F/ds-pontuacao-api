package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.core.gateway.RankingRedisGateway;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import br.com.ecociente.pontuacao.core.domain.ParticipanteRanking;

@Component
@RequiredArgsConstructor
public class RankingRedisGatewayImpl implements RankingRedisGateway {

  private static final long MAX_EXACT_SCORE = 9_007_199_254_740_991L;

  private static final DefaultRedisScript<Long> UPDATE_SCORE = new DefaultRedisScript<>("""
      local score = tonumber(ARGV[2])

      if score == 0 then
          redis.call('ZREM', KEYS[1], ARGV[1])
      else
          redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
      end

      redis.call('EXPIREAT', KEYS[1], ARGV[3])

      return 1
      """, Long.class);

  private final StringRedisTemplate redisTemplate;

  @Override
  public void definirPontuacao(
      String chave,
      Integer participanteId,
      Long pontos,
      OffsetDateTime expiraEm) {
    if (pontos == null || pontos < 0 || pontos > MAX_EXACT_SCORE) {
      throw new InconsistenciaPontuacaoException(
          "SCORE_INVALIDO",
          "A pontuação não pode ser representada no ranking");
    }

    Long resultado = redisTemplate.execute(
        UPDATE_SCORE,
        List.of(chave),
        participanteId.toString(),
        pontos.toString(),
        Long.toString(expiraEm.toEpochSecond()));

    if (!Long.valueOf(1L).equals(resultado)) {
      throw new IllegalStateException(
          "O Redis não confirmou a atualização do ranking");
    }
  }

  @Override
  public List<ParticipanteRanking> buscarTop(
      String chave,
      int limite) {

    Set<TypedTuple<String>> resultados = redisTemplate.opsForZSet()
        .reverseRangeWithScores(chave, 0, limite - 1);

    List<ParticipanteRanking> participantes = new ArrayList<>();

    if (resultados == null) {
      return participantes;
    }

    long posicao = 1;

    for (TypedTuple<String> resultado : resultados) {
      if (resultado.getValue() == null
          || resultado.getScore() == null) {
        throw new IllegalStateException(
            "Participante ou pontuação inválida no Redis");
      }

      participantes.add(
          ParticipanteRanking.builder()
              .posicao(posicao++)
              .participanteId(
                  Integer.valueOf(resultado.getValue()))
              .pontos(resultado.getScore().longValue())
              .build());
    }

    return participantes;
  }

  @Override
  public Long buscarPosicao(
      String chave,
      Integer participanteId) {

    Long indice = redisTemplate.opsForZSet()
        .reverseRank(chave, participanteId.toString());

    return indice == null ? null : indice + 1;
  }

  @Override
  public Long buscarPontuacao(
      String chave,
      Integer participanteId) {

    Double pontos = redisTemplate.opsForZSet()
        .score(chave, participanteId.toString());

    return pontos == null ? null : pontos.longValue();
  }

  @Override
  public Long contarParticipantes(String chave) {
    return redisTemplate.opsForZSet().zCard(chave);
  }

}