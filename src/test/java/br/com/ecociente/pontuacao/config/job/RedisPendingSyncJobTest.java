package br.com.ecociente.pontuacao.config.job;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;
import br.com.ecociente.pontuacao.core.service.SincronizacaoPontuacaoService;

@ExtendWith(MockitoExtension.class)
class RedisPendingSyncJobTest {

  private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

  @Mock
  private SincronizacaoPontuacaoGateway gateway;

  @Mock
  private SincronizacaoPontuacaoService service;

  @Mock
  private Clock clock;

  private RedisPendingSyncJob job;

  @BeforeEach
  void setUp() {
    job = new RedisPendingSyncJob(gateway, service, clock, 100);
    when(clock.instant()).thenReturn(NOW);
  }

  @Test
  @DisplayName("Deve sincronizar todas as movimentações do lote")
  void shouldSynchronizeAllPendingMovementsInBatch() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(100L, 101L));

    when(service.sincronizar(100L)).thenReturn(true);
    when(service.sincronizar(101L)).thenReturn(true);

    job.executar();

    verify(service).sincronizar(100L);
    verify(service).sincronizar(101L);
    verify(gateway, never()).registrarFalha(anyLong());
  }

  @Test
  @DisplayName("Não deve chamar o service quando não houver pendências")
  void shouldNotCallServiceWhenNoPendingMovementsExist() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of());

    job.executar();

    verifyNoInteractions(service);
  }

  @Test
  @DisplayName("Deve registrar falha e continuar processando o lote")
  void shouldContinueBatchAfterMovementFailure() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(100L, 101L));

    when(service.sincronizar(100L))
        .thenThrow(new IllegalStateException("Redis indisponível"));

    when(service.sincronizar(101L)).thenReturn(true);

    assertDoesNotThrow(() -> job.executar());

    verify(gateway).registrarFalha(100L);
    verify(service).sincronizar(101L);
  }

  @Test
  @DisplayName("Não deve registrar erro quando outro worker possui o bloqueio")
  void shouldNotRegisterFailureWhenMovementIsSkipped() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(100L));

    when(service.sincronizar(100L)).thenReturn(false);

    job.executar();

    verify(gateway, never()).registrarFalha(anyLong());
  }

  @Test
  @DisplayName("Deve aguardar o intervalo antes de tentar novamente")
  void shouldWaitBeforeRetryingFailedBatch() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(100L));

    when(service.sincronizar(100L))
        .thenThrow(new IllegalStateException("Redis indisponível"));

    job.executar();
    job.executar();

    verify(gateway, times(1)).buscarIdsPendentes(100);
    verify(service, times(1)).sincronizar(100L);
  }

  @Test
  @DisplayName("Deve dobrar a espera após falhas consecutivas")
  void shouldIncreaseDelayAfterConsecutiveFailures() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(100L));

    when(service.sincronizar(100L))
        .thenThrow(new IllegalStateException("Redis indisponível"));

    job.executar();

    when(clock.instant()).thenReturn(NOW.plusSeconds(5));
    job.executar();

    when(clock.instant()).thenReturn(NOW.plusSeconds(10));
    job.executar();

    verify(service, times(2)).sincronizar(100L);

    when(clock.instant()).thenReturn(NOW.plusSeconds(15));
    job.executar();

    verify(service, times(3)).sincronizar(100L);
  }

  @Test
  @DisplayName("Deve continuar o lote mesmo se o registro da falha falhar")
  void shouldContinueWhenFailureRegistrationAlsoFails() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(100L, 101L));

    when(service.sincronizar(100L))
        .thenThrow(new IllegalStateException("Redis indisponível"));

    doThrow(new IllegalStateException("Banco indisponível"))
        .when(gateway)
        .registrarFalha(100L);

    when(service.sincronizar(101L)).thenReturn(true);

    assertDoesNotThrow(() -> job.executar());

    verify(service).sincronizar(101L);
  }

  @Test
  @DisplayName("Deve tratar falha na consulta das pendências")
  void shouldHandlePendingMovementQueryFailure() {
    when(gateway.buscarIdsPendentes(100))
        .thenThrow(new IllegalStateException("Banco indisponível"));

    assertDoesNotThrow(() -> job.executar());

    verifyNoInteractions(service);
  }
}