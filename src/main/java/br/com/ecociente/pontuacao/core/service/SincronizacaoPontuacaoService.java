package br.com.ecociente.pontuacao.core.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.core.gateway.RankingRedisGateway;
import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;

@Service
public class SincronizacaoPontuacaoService {

  private final SincronizacaoPontuacaoGateway sincronizacaoGateway;
  private final RankingRedisGateway rankingGateway;
  private final CycleService cycleService;
  private final Clock clock;
  private final int retentionDays;

  public SincronizacaoPontuacaoService(
      SincronizacaoPontuacaoGateway sincronizacaoGateway,
      RankingRedisGateway rankingGateway,
      CycleService cycleService,
      Clock clock,
      @Value("${app.ranking.retention-days:90}") int retentionDays) {
    if (retentionDays < 1) {
      throw new IllegalArgumentException(
          "A retenção deve ser de pelo menos um dia");
    }

    this.sincronizacaoGateway = sincronizacaoGateway;
    this.rankingGateway = rankingGateway;
    this.cycleService = cycleService;
    this.clock = clock;
    this.retentionDays = retentionDays;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
  public boolean sincronizar(Long movimentacaoId) {
    if (!sincronizacaoGateway.tentarBloquearSincronizacao()) {
      return false;
    }

    MovimentacaoPontos movimentacao = sincronizacaoGateway
        .buscarPorId(movimentacaoId)
        .orElseThrow(() -> new NotFoundException(
            "MOVIMENTACAO_NAO_ENCONTRADA",
            "Movimentação não encontrada"));

    if (Boolean.TRUE.equals(movimentacao.getRedisSincronizado())) {
      return false;
    }

    MovimentacaoPontos credito = buscarCreditoOriginal(movimentacao);

    if (credito.getCondominioId() == null) {
      if (credito.getTorreId() != null) {
        throw new InconsistenciaPontuacaoException(
            "CONTEXTO_CONDOMINIAL_INVALIDO",
            "A movimentação possui torre sem condomínio");
      }

      marcarSincronizada(movimentacaoId);
      return true;
    }

    atualizarMorador(credito);

    if (credito.getTorreId() != null) {
      atualizarTorre(credito);
    }

    marcarSincronizada(movimentacaoId);
    return true;
  }

  private MovimentacaoPontos buscarCreditoOriginal(
      MovimentacaoPontos movimentacao) {
    if (movimentacao.getTipoMovimentacao() == TipoMovimentacaoType.CREDITO) {
      return movimentacao;
    }

    if (movimentacao.getMovimentacaoReferenciaId() == null) {
      throw new InconsistenciaPontuacaoException(
          "CREDITO_ORIGINAL_AUSENTE",
          "O ajuste não referencia o crédito original");
    }

    MovimentacaoPontos credito = sincronizacaoGateway
        .buscarPorId(movimentacao.getMovimentacaoReferenciaId())
        .orElseThrow(() -> new InconsistenciaPontuacaoException(
            "CREDITO_ORIGINAL_AUSENTE",
            "O crédito original não foi encontrado"));

    if (credito.getTipoMovimentacao() != TipoMovimentacaoType.CREDITO) {
      throw new InconsistenciaPontuacaoException(
          "REFERENCIA_INVALIDA",
          "A referência do ajuste não é um crédito");
    }

    return credito;
  }

  private void atualizarMorador(MovimentacaoPontos credito) {
    CicloRanking ciclo = cycleService.calcular(
        credito.getOcorridoEm(),
        TipoRankingType.MORADORES);

    Long pontos = sincronizacaoGateway.calcularSaldoMorador(
        credito.getUsuarioId(),
        credito.getCondominioId(),
        ciclo);

    validarSaldo(pontos);

    OffsetDateTime expiraEm = ciclo.getFim().plusDays(retentionDays);

    if (!expiraEm.isAfter(OffsetDateTime.now(clock))) {
      return;
    }

    rankingGateway.definirPontuacao(
        cycleService.montarChave(
            TipoRankingType.MORADORES,
            credito.getCondominioId(),
            ciclo),
        credito.getUsuarioId(),
        pontos,
        expiraEm);
  }

  private void atualizarTorre(MovimentacaoPontos credito) {
    CicloRanking ciclo = cycleService.calcular(
        credito.getOcorridoEm(),
        TipoRankingType.TORRES);

    Long pontos = sincronizacaoGateway.calcularSaldoTorre(
        credito.getTorreId(),
        credito.getCondominioId(),
        ciclo);

    validarSaldo(pontos);

    OffsetDateTime expiraEm = ciclo.getFim().plusDays(retentionDays);

    if (!expiraEm.isAfter(OffsetDateTime.now(clock))) {
      return;
    }

    rankingGateway.definirPontuacao(
        cycleService.montarChave(
            TipoRankingType.TORRES,
            credito.getCondominioId(),
            ciclo),
        credito.getTorreId(),
        pontos,
        expiraEm);
  }

  private void validarSaldo(Long pontos) {
    if (pontos == null || pontos < 0) {
      throw new InconsistenciaPontuacaoException(
          "INCONSISTENCIA_MOVIMENTACAO",
          "O histórico produziu um saldo inválido para o ranking");
    }
  }

  private void marcarSincronizada(Long movimentacaoId) {
    sincronizacaoGateway.marcarSincronizada(
        movimentacaoId,
        OffsetDateTime.now(clock));
  }
}