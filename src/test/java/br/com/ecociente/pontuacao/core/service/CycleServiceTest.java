package br.com.ecociente.pontuacao.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.ecociente.pontuacao.core.domain.TipoRankingType;

class CycleServiceTest {

  private CycleService service;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(
        Instant.parse("2026-01-12T15:00:00Z"),
        ZoneId.of("America/Sao_Paulo"));

    service = new CycleService(clock, "2026-01-05");
  }

  @Test
  @DisplayName("Deve calcular ciclo de sete dias para moradores")
  void shouldCalculateSevenDayResidentCycle() {
    var ciclo = service.calcular(
        OffsetDateTime.parse("2026-01-10T12:00:00-03:00"),
        TipoRankingType.MORADORES);

    assertEquals("2026-01-05_7d", ciclo.getId());
    assertEquals(
        OffsetDateTime.parse("2026-01-05T00:00:00-03:00"),
        ciclo.getInicio());
    assertEquals(
        OffsetDateTime.parse("2026-01-12T00:00:00-03:00"),
        ciclo.getFim());
  }

  @Test
  @DisplayName("Deve iniciar novo ciclo exatamente no limite")
  void shouldStartNewCycleAtBoundary() {
    var ciclo = service.calcular(
        OffsetDateTime.parse("2026-01-12T00:00:00-03:00"),
        TipoRankingType.MORADORES);

    assertEquals("2026-01-12_7d", ciclo.getId());
  }

  @Test
  @DisplayName("Deve calcular ciclo de trinta dias para torres")
  void shouldCalculateThirtyDayTowerCycle() {
    var ciclo = service.calcular(
        OffsetDateTime.parse("2026-02-03T23:59:59-03:00"),
        TipoRankingType.TORRES);

    assertEquals("2026-01-05_30d", ciclo.getId());
    assertEquals(
        OffsetDateTime.parse("2026-02-04T00:00:00-03:00"),
        ciclo.getFim());
  }

  @Test
  @DisplayName("Deve converter a referência para o timezone da aplicação")
  void shouldUseApplicationTimezone() {
    var ciclo = service.calcular(
        OffsetDateTime.parse("2026-01-12T02:00:00Z"),
        TipoRankingType.MORADORES);

    assertEquals("2026-01-05_7d", ciclo.getId());
  }

  @Test
  @DisplayName("Deve calcular corretamente datas anteriores ao marco inicial")
  void shouldCalculateCycleBeforeEpoch() {
    var ciclo = service.calcular(
        OffsetDateTime.parse("2026-01-04T12:00:00-03:00"),
        TipoRankingType.MORADORES);

    assertEquals("2025-12-29_7d", ciclo.getId());
    assertEquals(
        OffsetDateTime.parse("2026-01-05T00:00:00-03:00"),
        ciclo.getFim());
  }

  @Test
  @DisplayName("Deve utilizar o relógio para consultar o ciclo atual")
  void shouldCalculateCurrentCycleUsingClock() {
    var ciclo = service.atual(TipoRankingType.MORADORES);

    assertEquals("2026-01-12_7d", ciclo.getId());
  }

  @Test
  @DisplayName("Deve incluir condomínio e ciclo na chave de moradores")
  void shouldBuildResidentKeyWithCondominiumAndCycle() {
    var ciclo = service.atual(TipoRankingType.MORADORES);

    String chave = service.montarChave(
        TipoRankingType.MORADORES,
        7,
        ciclo);

    assertEquals(
        "ecociente:ranking:moradores:7:2026-01-12_7d",
        chave);
  }

  @Test
  @DisplayName("Deve incluir condomínio e ciclo na chave de torres")
  void shouldBuildTowerKeyWithCondominiumAndCycle() {
    var ciclo = service.atual(TipoRankingType.TORRES);

    String chave = service.montarChave(
        TipoRankingType.TORRES,
        9,
        ciclo);

    assertEquals(
        "ecociente:ranking:torres:9:2026-01-05_30d",
        chave);
  }
}