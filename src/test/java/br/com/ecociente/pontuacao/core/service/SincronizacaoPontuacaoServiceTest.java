package br.com.ecociente.pontuacao.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.core.gateway.RankingRedisGateway;
import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;

@ExtendWith(MockitoExtension.class)
class SincronizacaoPontuacaoServiceTest {

  private static final String RESIDENT_KEY = "ranking:moradores:7";
  private static final String TOWER_KEY = "ranking:torres:7";

  @Mock
  private SincronizacaoPontuacaoGateway sincronizacaoGateway;

  @Mock
  private RankingRedisGateway rankingGateway;

  @Mock
  private CycleService cycleService;

  private SincronizacaoPontuacaoService service;
  private Clock clock;

  private MovimentacaoPontos credito;
  private CicloRanking cicloMoradores;
  private CicloRanking cicloTorres;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(
        Instant.parse("2026-10-10T12:00:00Z"),
        ZoneId.of("America/Sao_Paulo"));

    service = new SincronizacaoPontuacaoService(
        sincronizacaoGateway,
        rankingGateway,
        cycleService,
        clock,
        90);

    credito = MovimentacaoPontos.builder()
        .id(100L)
        .usuarioId(42)
        .condominioId(7)
        .torreId(2)
        .tipoMovimentacao(TipoMovimentacaoType.CREDITO)
        .pontos(10)
        .ocorridoEm(
            OffsetDateTime.parse("2026-10-09T10:00:00-03:00"))
        .redisSincronizado(false)
        .build();

    cicloMoradores = CicloRanking.builder()
        .id("2026-10-05_7d")
        .inicio(
            OffsetDateTime.parse("2026-10-05T00:00:00-03:00"))
        .fim(
            OffsetDateTime.parse("2026-10-12T00:00:00-03:00"))
        .build();

    cicloTorres = CicloRanking.builder()
        .id("2026-10-02_30d")
        .inicio(
            OffsetDateTime.parse("2026-10-02T00:00:00-03:00"))
        .fim(
            OffsetDateTime.parse("2026-11-01T00:00:00-03:00"))
        .build();
  }

  @Test
  @DisplayName("Deve atualizar morador e torre antes de marcar sincronização")
  void shouldUpdateBothRankingsBeforeMarkingMovementAsSynchronized() {
    prepareMovement(credito);
    prepareResidentCycle(120L);
    prepareTowerCycle(450L);

    boolean resultado = service.sincronizar(100L);

    assertTrue(resultado);

    var ordem = inOrder(rankingGateway, sincronizacaoGateway);

    ordem.verify(rankingGateway).definirPontuacao(
        RESIDENT_KEY,
        42,
        120L,
        cicloMoradores.getFim().plusDays(90));

    ordem.verify(rankingGateway).definirPontuacao(
        TOWER_KEY,
        2,
        450L,
        cicloTorres.getFim().plusDays(90));

    ordem.verify(sincronizacaoGateway).marcarSincronizada(
        100L,
        OffsetDateTime.now(clock));
  }

  @Test
  @DisplayName("Deve ignorar a movimentação quando outro worker possui o bloqueio")
  void shouldSkipMovementWhenLockIsUnavailable() {
    when(sincronizacaoGateway.tentarBloquearSincronizacao())
        .thenReturn(false);

    assertFalse(service.sincronizar(100L));

    verify(sincronizacaoGateway, never()).buscarPorId(anyLong());
    verifyNoInteractions(rankingGateway, cycleService);
  }

  @Test
  @DisplayName("Deve ignorar movimentação já sincronizada")
  void shouldSkipAlreadySynchronizedMovement() {
    credito.setRedisSincronizado(true);
    prepareMovement(credito);

    assertFalse(service.sincronizar(100L));

    verifyNoInteractions(rankingGateway, cycleService);
    verify(sincronizacaoGateway, never())
        .marcarSincronizada(anyLong(), any());
  }

  @Test
  @DisplayName("Deve lançar exceção para movimentação inexistente")
  void shouldThrowWhenMovementDoesNotExist() {
    when(sincronizacaoGateway.tentarBloquearSincronizacao())
        .thenReturn(true);

    when(sincronizacaoGateway.buscarPorId(100L))
        .thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> service.sincronizar(100L));

    assertEquals("MOVIMENTACAO_NAO_ENCONTRADA", ex.getCodigoErro());
    verifyNoInteractions(rankingGateway);
  }

  @Test
  @DisplayName("Deve concluir crédito pessoal sem criar ranking condominial")
  void shouldCompletePersonalCreditWithoutUpdatingRankings() {
    credito.setCondominioId(null);
    credito.setTorreId(null);

    prepareMovement(credito);

    assertTrue(service.sincronizar(100L));

    verifyNoInteractions(rankingGateway, cycleService);
    verify(sincronizacaoGateway).marcarSincronizada(
        100L,
        OffsetDateTime.now(clock));
  }

  @Test
  @DisplayName("Deve rejeitar torre sem condomínio")
  void shouldRejectTowerWithoutCondominium() {
    credito.setCondominioId(null);
    prepareMovement(credito);

    InconsistenciaPontuacaoException ex = assertThrows(
        InconsistenciaPontuacaoException.class,
        () -> service.sincronizar(100L));

    assertEquals(
        "CONTEXTO_CONDOMINIAL_INVALIDO",
        ex.getCodigoErro());

    verifyNoInteractions(rankingGateway);
    verify(sincronizacaoGateway, never())
        .marcarSincronizada(anyLong(), any());
  }

  @Test
  @DisplayName("Deve atualizar somente moradores quando não houver torre")
  void shouldUpdateOnlyResidentRankingWhenTowerIsMissing() {
    credito.setTorreId(null);

    prepareMovement(credito);
    prepareResidentCycle(120L);

    assertTrue(service.sincronizar(100L));

    verify(rankingGateway).definirPontuacao(
        RESIDENT_KEY,
        42,
        120L,
        cicloMoradores.getFim().plusDays(90));

    verify(sincronizacaoGateway, never())
        .calcularSaldoTorre(anyInt(), anyInt(), any());

    verifyNoMoreInteractions(rankingGateway);
  }

  @ParameterizedTest
  @EnumSource(value = TipoMovimentacaoType.class, names = { "ESTORNO", "RESTAURACAO" })
  @DisplayName("Deve usar a data do crédito original para sincronizar ajustes")
  void shouldUseOriginalCreditDateForAdjustments(
      TipoMovimentacaoType movementType) {
    credito.setTorreId(null);

    MovimentacaoPontos ajuste = MovimentacaoPontos.builder()
        .id(101L)
        .tipoMovimentacao(movementType)
        .movimentacaoReferenciaId(100L)
        .ocorridoEm(
            OffsetDateTime.parse("2026-10-10T09:00:00-03:00"))
        .redisSincronizado(false)
        .build();

    prepareMovement(ajuste);

    when(sincronizacaoGateway.buscarPorId(100L))
        .thenReturn(Optional.of(credito));

    prepareResidentCycle(110L);

    assertTrue(service.sincronizar(101L));

    verify(cycleService).calcular(
        credito.getOcorridoEm(),
        TipoRankingType.MORADORES);

    verify(cycleService, never()).calcular(
        ajuste.getOcorridoEm(),
        TipoRankingType.MORADORES);

    verify(sincronizacaoGateway).marcarSincronizada(
        101L,
        OffsetDateTime.now(clock));
  }

  @Test
  @DisplayName("Deve rejeitar ajuste sem referência ao crédito")
  void shouldRejectAdjustmentWithoutOriginalCreditReference() {
    credito.setTipoMovimentacao(TipoMovimentacaoType.ESTORNO);
    credito.setMovimentacaoReferenciaId(null);

    prepareMovement(credito);

    InconsistenciaPontuacaoException ex = assertThrows(
        InconsistenciaPontuacaoException.class,
        () -> service.sincronizar(100L));

    assertEquals("CREDITO_ORIGINAL_AUSENTE", ex.getCodigoErro());
    verifyNoInteractions(rankingGateway);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(longs = { -1L })
  @DisplayName("Deve rejeitar saldo inválido calculado pelo banco")
  void shouldRejectInvalidCalculatedBalance(Long balance) {
    credito.setTorreId(null);
    prepareMovement(credito);

    when(cycleService.calcular(
        credito.getOcorridoEm(),
        TipoRankingType.MORADORES)).thenReturn(cicloMoradores);

    when(sincronizacaoGateway.calcularSaldoMorador(
        42,
        7,
        cicloMoradores)).thenReturn(balance);

    InconsistenciaPontuacaoException ex = assertThrows(
        InconsistenciaPontuacaoException.class,
        () -> service.sincronizar(100L));

    assertEquals("INCONSISTENCIA_MOVIMENTACAO", ex.getCodigoErro());

    verifyNoInteractions(rankingGateway);
    verify(sincronizacaoGateway, never())
        .marcarSincronizada(anyLong(), any());
  }

  @Test
  @DisplayName("Não deve recriar ciclo cujo prazo de retenção já terminou")
  void shouldNotRecreateExpiredCycle() {
    credito.setTorreId(null);

    cicloMoradores.setFim(
        OffsetDateTime.parse("2026-01-12T00:00:00-03:00"));

    prepareMovement(credito);

    when(cycleService.calcular(
        credito.getOcorridoEm(),
        TipoRankingType.MORADORES)).thenReturn(cicloMoradores);

    when(sincronizacaoGateway.calcularSaldoMorador(
        42,
        7,
        cicloMoradores)).thenReturn(10L);

    assertTrue(service.sincronizar(100L));

    verifyNoInteractions(rankingGateway);

    verify(sincronizacaoGateway).marcarSincronizada(
        100L,
        OffsetDateTime.now(clock));
  }

  @Test
  @DisplayName("Não deve marcar sincronização se a atualização da torre falhar")
  void shouldKeepMovementPendingWhenTowerUpdateFails() {
    prepareMovement(credito);
    prepareResidentCycle(120L);
    prepareTowerCycle(450L);

    doNothing()
        .when(rankingGateway)
        .definirPontuacao(
            RESIDENT_KEY,
            42,
            120L,
            cicloMoradores.getFim().plusDays(90));

    doThrow(new IllegalStateException("Redis indisponível"))
        .when(rankingGateway)
        .definirPontuacao(
            TOWER_KEY,
            2,
            450L,
            cicloTorres.getFim().plusDays(90));

    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> service.sincronizar(100L));

    assertEquals("Redis indisponível", ex.getMessage());

    var ordem = inOrder(rankingGateway);

    ordem.verify(rankingGateway).definirPontuacao(
        RESIDENT_KEY,
        42,
        120L,
        cicloMoradores.getFim().plusDays(90));

    ordem.verify(rankingGateway).definirPontuacao(
        TOWER_KEY,
        2,
        450L,
        cicloTorres.getFim().plusDays(90));

    verify(sincronizacaoGateway, never())
        .marcarSincronizada(anyLong(), any());
  }

  @Test
  @DisplayName("Deve reenviar o saldo absoluto ao repetir uma tentativa pendente")
  void shouldResendAbsoluteBalanceWhenRetryingPendingMovement() {
    credito.setTorreId(null);

    prepareMovement(credito);
    prepareResidentCycle(120L);

    doThrow(new IllegalStateException("Falha de comunicação"))
        .doNothing()
        .when(rankingGateway)
        .definirPontuacao(
            RESIDENT_KEY,
            42,
            120L,
            cicloMoradores.getFim().plusDays(90));

    assertThrows(
        IllegalStateException.class,
        () -> service.sincronizar(100L));

    assertTrue(service.sincronizar(100L));

    verify(rankingGateway, times(2)).definirPontuacao(
        RESIDENT_KEY,
        42,
        120L,
        cicloMoradores.getFim().plusDays(90));

    verify(sincronizacaoGateway, times(1)).marcarSincronizada(
        100L,
        OffsetDateTime.now(clock));
  }

  private void prepareMovement(MovimentacaoPontos movement) {
    when(sincronizacaoGateway.tentarBloquearSincronizacao())
        .thenReturn(true);

    when(sincronizacaoGateway.buscarPorId(movement.getId()))
        .thenReturn(Optional.of(movement));
  }

  private void prepareResidentCycle(Long balance) {
    when(cycleService.calcular(
        credito.getOcorridoEm(),
        TipoRankingType.MORADORES)).thenReturn(cicloMoradores);

    when(sincronizacaoGateway.calcularSaldoMorador(
        42,
        7,
        cicloMoradores)).thenReturn(balance);

    when(cycleService.montarChave(
        TipoRankingType.MORADORES,
        7,
        cicloMoradores)).thenReturn(RESIDENT_KEY);
  }

  private void prepareTowerCycle(Long balance) {
    when(cycleService.calcular(
        credito.getOcorridoEm(),
        TipoRankingType.TORRES)).thenReturn(cicloTorres);

    when(sincronizacaoGateway.calcularSaldoTorre(
        2,
        7,
        cicloTorres)).thenReturn(balance);

    when(cycleService.montarChave(
        TipoRankingType.TORRES,
        7,
        cicloTorres)).thenReturn(TOWER_KEY);
  }
}