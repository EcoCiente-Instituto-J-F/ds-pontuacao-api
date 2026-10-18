
package br.com.ecociente.pontuacao.core.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;
import br.com.ecociente.pontuacao.core.exception.BusinessException;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.dataprovaider.repository.MovimentacaoPontosRepository;
import br.com.ecociente.pontuacao.entrypoint.dto.request.EscopoReconstrucaoType;

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

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ResultadoReconstrucao reconstruir(
      EscopoReconstrucaoType tipo,
      Integer condominioId) {
    if (!repository.tentarBloquearSincronizacao()) {
      throw new BusinessException(
          "RANKING_EM_PROCESSAMENTO",
          "Há uma sincronização ou reconstrução em andamento. Tente novamente");
    }

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

    OffsetDateTime expiraEm = ciclo.getFim().plusDays(retentionDays);

    if (!expiraEm.isAfter(OffsetDateTime.now(clock))) {
      throw new IllegalStateException(
          "O ciclo já ultrapassou a retenção");
    }

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
    RuntimeException falhaPrincipal = null;

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
          Boolean adicionado = redisTemplate.opsForZSet().add(
              temporaria,
              participanteId,
              pontos);

          if (!Boolean.TRUE.equals(adicionado)) {
            throw new IllegalStateException(
                "Não foi possível montar o ranking temporário");
          }

          total++;

          if (total == 1) {
            Boolean expiracaoDefinida = redisTemplate.expireAt(
                temporaria,
                expiraEm.toInstant());

            if (!Boolean.TRUE.equals(expiracaoDefinida)) {
              throw new IllegalStateException(
                  "Não foi possível definir a retenção do ranking");
            }
          }
        }
      }

      if (total == 0) {
        redisTemplate.delete(chave);
      } else {
        redisTemplate.rename(temporaria, chave);
      }

      return total;

    } catch (RuntimeException exception) {
      falhaPrincipal = exception;
      throw exception;

    } finally {
      try {
        redisTemplate.delete(temporaria);
      } catch (RuntimeException limpezaException) {
        if (falhaPrincipal != null) {
          falhaPrincipal.addSuppressed(limpezaException);
        } else {
          throw limpezaException;
        }
      }
    }
  }

  public record ResultadoReconstrucao(
      String tipo,
      Integer condominioId,
      int participantes) {
  }
}
