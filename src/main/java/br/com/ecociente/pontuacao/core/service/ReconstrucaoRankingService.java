
package br.com.ecociente.pontuacao.core.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.dataprovaider.repository.MovimentacaoPontosRepository;
import br.com.ecociente.pontuacao.entrypoint.dto.request.EscopoReconstrucaoType;

import lombok.RequiredArgsConstructor;

@Service
public class ReconstrucaoRankingService {

  private final MovimentacaoPontosRepository repository;
  private final StringRedisTemplate redisTemplate;
  private final CycleService cycleService;
  private final Clock clock;
  private final int retentionDays;

  public ReconstrucaoRankingService(
      MovimentacaoPontosRepository repository,
      StringRedisTemplate redisTemplate,
      CycleService cycleService,
      Clock clock,
      @Value("${app.ranking.retention-days:90}") int retentionDays) {

    this.repository = repository;
    this.redisTemplate = redisTemplate;
    this.cycleService = cycleService;
    this.clock = clock;
    this.retentionDays = retentionDays;
  }

  public ResultadoReconstrucao reconstruir(
      EscopoReconstrucaoType tipo,
      Integer condominioId) {

    int participantes = 0;

    if (tipo == EscopoReconstrucaoType.MORADORES
        || tipo == EscopoReconstrucaoType.TODOS) {

      participantes += reconstruirTipo(
          TipoRankingType.MORADORES,
          condominioId);
    }

    if (tipo == EscopoReconstrucaoType.TORRES
        || tipo == EscopoReconstrucaoType.TODOS) {

      participantes += reconstruirTipo(
          TipoRankingType.TORRES,
          condominioId);
    }

    return new ResultadoReconstrucao(
        tipo.name(),
        condominioId,
        participantes);
  }

  private int reconstruirTipo(
      TipoRankingType tipo,
      Integer condominioId) {

    CicloRanking ciclo = cycleService.atual(tipo);

    List<Object[]> saldos;

    if (tipo == TipoRankingType.MORADORES) {
      saldos = repository.agruparMoradoresNoCiclo(
          condominioId,
          ciclo.getInicio(),
          ciclo.getFim());
    } else {
      saldos = repository.agruparTorresNoCiclo(
          condominioId,
          ciclo.getInicio(),
          ciclo.getFim());
    }

    String chave = cycleService.montarChave(
        tipo,
        condominioId,
        ciclo);

    String temporaria = chave + ":tmp:" + UUID.randomUUID();

    int total = 0;

    try {
      for (Object[] saldo : saldos) {
        String participanteId = String.valueOf(((Number) saldo[0]).intValue());

        long pontos = ((Number) saldo[1]).longValue();

        if (pontos < 0) {
          throw new InconsistenciaPontuacaoException(
              "INCONSISTENCIA_MOVIMENTACAO",
              "O ranking possui saldo negativo");
        }

        if (pontos > 0) {
          redisTemplate.opsForZSet().add(
              temporaria,
              participanteId,
              pontos);

          total++;
        }
      }

      OffsetDateTime expiraEm = ciclo.getFim().plusDays(retentionDays);

      if (!expiraEm.isAfter(OffsetDateTime.now(clock))) {
        throw new IllegalStateException(
            "O ciclo já ultrapassou a retenção");
      }

      if (total == 0) {
        redisTemplate.delete(chave);
      } else {
        redisTemplate.expireAt(
            temporaria,
            expiraEm.toInstant());

        Boolean renomeada = redisTemplate.renameIfAbsent(
            temporaria,
            chave + ":swap");

        if (!Boolean.TRUE.equals(renomeada)) {
          throw new IllegalStateException(
              "Já existe uma reconstrução intermediária");
        }

        redisTemplate.rename(
            chave + ":swap",
            chave);
      }

      return total;

    } finally {
      redisTemplate.delete(temporaria);
    }
  }

  public record ResultadoReconstrucao(
      String tipo,
      Integer condominioId,
      int participantes) {
  }
}
