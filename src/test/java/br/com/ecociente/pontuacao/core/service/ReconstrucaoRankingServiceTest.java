
package br.com.ecociente.pontuacao.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.dataprovaider.repository.MovimentacaoPontosRepository;
import br.com.ecociente.pontuacao.entrypoint.dto.request.EscopoReconstrucaoType;

class ReconstrucaoRankingServiceTest {

  private MovimentacaoPontosRepository repository;
  private StringRedisTemplate redisTemplate;
  private ZSetOperations<String, String> zSet;
  private CycleService cycleService;
  private ReconstrucaoRankingService service;

  @BeforeEach
  void configurar() {
    repository = mock(MovimentacaoPontosRepository.class);
    redisTemplate = mock(StringRedisTemplate.class);
    zSet = mock(ZSetOperations.class);
    cycleService = mock(CycleService.class);

    Clock clock = Clock.fixed(
        Instant.parse("2026-10-10T12:00:00Z"),
        ZoneId.of("America/Sao_Paulo"));

    service = new ReconstrucaoRankingService(
        repository,
        redisTemplate,
        cycleService,
        clock,
        90);

    when(redisTemplate.opsForZSet()).thenReturn(zSet);

    CicloRanking ciclo = CicloRanking.builder()
        .id("2026-10-05_7d")
        .inicio(OffsetDateTime.parse("2026-10-05T00:00:00-03:00"))
        .fim(OffsetDateTime.parse("2026-10-12T00:00:00-03:00"))
        .build();

    when(cycleService.atual(TipoRankingType.MORADORES))
        .thenReturn(ciclo);

    when(cycleService.montarChave(
        TipoRankingType.MORADORES, 7, ciclo)).thenReturn("ranking:moradores:7");

    when(redisTemplate.renameIfAbsent(
        anyString(), eq("ranking:moradores:7:swap"))).thenReturn(true);
  }

  @Test
  void deveReconstruirScoresDosMoradores() {
    when(repository.agruparMoradoresNoCiclo(
        eq(7), any(), any())).thenReturn(List.of(
            new Object[] { 42, 100L },
            new Object[] { 15, 50L }));

    var resultado = service.reconstruir(
        EscopoReconstrucaoType.MORADORES, 7);

    assertEquals(2, resultado.participantes());

    verify(zSet).add(
        contains(":tmp:"), eq("42"), eq(100.0d));

    verify(zSet).add(
        contains(":tmp:"), eq("15"), eq(50.0d));
  }

  @Test
  void deveIgnorarPontuacaoZero() {
    when(repository.agruparMoradoresNoCiclo(
        eq(7), any(), any())).thenReturn(List.<Object[]>of(
            new Object[] { 42, 0L },
            new Object[] { 15, 30L }));

    var resultado = service.reconstruir(
        EscopoReconstrucaoType.MORADORES, 7);

    assertEquals(1, resultado.participantes());
    verify(zSet).add(
        contains(":tmp:"), eq("15"), eq(30.0d));
  }

  @Test
  void deveRejeitarSaldoNegativo() {
    when(repository.agruparMoradoresNoCiclo(
        eq(7), any(), any())).thenReturn(List.<Object[]>of(
            new Object[] { 42, -10L }));

    assertThrows(
        InconsistenciaPontuacaoException.class,
        () -> service.reconstruir(
            EscopoReconstrucaoType.MORADORES, 7));

    verify(redisTemplate, never()).rename(
        anyString(), anyString());
  }

  @Test
  void deveRemoverRankingQuandoNaoExistemParticipantes() {
    when(repository.agruparMoradoresNoCiclo(
        eq(7), any(), any())).thenReturn(List.of());

    var resultado = service.reconstruir(
        EscopoReconstrucaoType.MORADORES, 7);

    assertEquals(0, resultado.participantes());

    verify(redisTemplate).delete("ranking:moradores:7");
    verify(redisTemplate, never()).rename(
        anyString(), anyString());
  }

  @Test
  void naoDeveSubstituirRankingSeTrocaIntermediariaFalhar() {
    when(repository.agruparMoradoresNoCiclo(
        eq(7), any(), any())).thenReturn(List.<Object[]>of(
            new Object[] { 42, 100L }));

    when(redisTemplate.renameIfAbsent(
        anyString(), eq("ranking:moradores:7:swap"))).thenReturn(false);

    assertThrows(
        IllegalStateException.class,
        () -> service.reconstruir(
            EscopoReconstrucaoType.MORADORES, 7));

    verify(redisTemplate, never()).rename(
        "ranking:moradores:7:swap",
        "ranking:moradores:7");
  }
}
