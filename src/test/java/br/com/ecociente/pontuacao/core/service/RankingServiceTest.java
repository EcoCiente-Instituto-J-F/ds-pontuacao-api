
package br.com.ecociente.pontuacao.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.ecociente.pontuacao.core.domain.*;
import br.com.ecociente.pontuacao.core.gateway.*;

class RankingServiceTest {

  private RankingRedisGateway rankingGateway;
  private CycleService cycleService;
  private ParticipanteRankingGateway participanteGateway;
  private RankingService service;

  private CicloRanking ciclo;

  @BeforeEach
  void configurar() {
    rankingGateway = mock(RankingRedisGateway.class);
    cycleService = mock(CycleService.class);
    participanteGateway = mock(ParticipanteRankingGateway.class);

    service = new RankingService(
        rankingGateway,
        cycleService,
        participanteGateway);

    ciclo = CicloRanking.builder()
        .id("2026-10-05_7d")
        .inicio(OffsetDateTime.parse("2026-10-05T00:00:00-03:00"))
        .fim(OffsetDateTime.parse("2026-10-12T00:00:00-03:00"))
        .build();
  }

  private void configurarChave(TipoRankingType tipo) {
    when(cycleService.atual(tipo)).thenReturn(ciclo);
    when(cycleService.montarChave(tipo, 7, ciclo))
        .thenReturn("ranking:teste");
  }

  @Test
  void deveConsultarMoradoresComNomes() {
    configurarChave(TipoRankingType.MORADORES);

    ParticipanteRanking participante = ParticipanteRanking.builder()
        .posicao(1L)
        .participanteId(42)
        .pontos(100L)
        .build();

    when(rankingGateway.buscarTop("ranking:teste", 10))
        .thenReturn(List.of(participante));

    when(participanteGateway.buscarNomeMorador(42))
        .thenReturn(Optional.of("Ana"));

    Ranking resultado = service.consultar(
        TipoRankingType.MORADORES, 7, 10);

    assertEquals(TipoRankingType.MORADORES, resultado.getTipo());
    assertEquals(7, resultado.getCondominioId());
    assertEquals(1, resultado.getParticipantes().size());

    ParticipanteRanking primeiro = resultado.getParticipantes().get(0);

    assertEquals(42, primeiro.getParticipanteId());
    assertEquals("Ana", primeiro.getNome());
    assertEquals(100L, primeiro.getPontos());

    verify(participanteGateway).buscarNomeMorador(42);
    verify(participanteGateway, never())
        .buscarNomeTorre(anyInt(), anyInt());
  }

  @Test
  void deveConsultarTorresComNomes() {
    configurarChave(TipoRankingType.TORRES);

    ParticipanteRanking participante = ParticipanteRanking.builder()
        .posicao(1L)
        .participanteId(5)
        .pontos(200L)
        .build();

    when(rankingGateway.buscarTop("ranking:teste", 10))
        .thenReturn(List.of(participante));

    when(participanteGateway.buscarNomeTorre(5, 7))
        .thenReturn(Optional.of("Torre A"));

    Ranking resultado = service.consultar(
        TipoRankingType.TORRES, 7, 10);

    assertEquals("Torre A",
        resultado.getParticipantes().get(0).getNome());

    assertEquals(200L,
        resultado.getParticipantes().get(0).getPontos());

    verify(participanteGateway).buscarNomeTorre(5, 7);
    verify(participanteGateway, never())
        .buscarNomeMorador(anyInt());
  }

  @Test
  void deveRetornarRankingVazio() {
    configurarChave(TipoRankingType.MORADORES);

    when(rankingGateway.buscarTop("ranking:teste", 10))
        .thenReturn(List.of());

    Ranking resultado = service.consultar(
        TipoRankingType.MORADORES, 7, 10);

    assertTrue(resultado.getParticipantes().isEmpty());
    verifyNoInteractions(participanteGateway);
  }

  @Test
  void deveManterNomeNuloQuandoNaoEncontrado() {
    configurarChave(TipoRankingType.MORADORES);

    ParticipanteRanking participante = ParticipanteRanking.builder()
        .posicao(1L)
        .participanteId(42)
        .pontos(10L)
        .build();

    when(rankingGateway.buscarTop("ranking:teste", 10))
        .thenReturn(List.of(participante));

    when(participanteGateway.buscarNomeMorador(42))
        .thenReturn(Optional.empty());

    Ranking resultado = service.consultar(
        TipoRankingType.MORADORES, 7, 10);

    assertNull(resultado.getParticipantes().get(0).getNome());
  }

  @Test
  void deveConsultarPosicaoIndividual() {
    configurarChave(TipoRankingType.MORADORES);

    when(rankingGateway.buscarPosicao("ranking:teste", 42))
        .thenReturn(3L);

    Long posicao = service.consultarPosicao(
        TipoRankingType.MORADORES, 7, 42);

    assertEquals(3L, posicao);
  }

  @Test
  void deveRetornarNuloParaPosicaoInexistente() {
    configurarChave(TipoRankingType.MORADORES);

    when(rankingGateway.buscarPosicao("ranking:teste", 999))
        .thenReturn(null);

    Long posicao = service.consultarPosicao(
        TipoRankingType.MORADORES, 7, 999);

    assertNull(posicao);

    verify(rankingGateway).buscarPosicao("ranking:teste", 999);
  }

  @Test
  void deveConsultarPontuacaoIndividual() {
    configurarChave(TipoRankingType.TORRES);

    when(rankingGateway.buscarPontuacao("ranking:teste", 5))
        .thenReturn(150L);

    Long pontos = service.consultarPontuacao(
        TipoRankingType.TORRES, 7, 5);

    assertEquals(150L, pontos);
  }

  @Test
  void deveContarParticipantes() {
    configurarChave(TipoRankingType.MORADORES);

    when(rankingGateway.contarParticipantes("ranking:teste"))
        .thenReturn(25L);

    assertEquals(25L, service.contarParticipantes(
        TipoRankingType.MORADORES, 7));
  }
}
