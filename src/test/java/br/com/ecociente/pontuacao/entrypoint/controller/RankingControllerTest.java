
package br.com.ecociente.pontuacao.entrypoint.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.ecociente.pontuacao.config.AutorizacaoPontuacao;
import br.com.ecociente.pontuacao.core.domain.*;
import br.com.ecociente.pontuacao.core.service.RankingService;

class RankingControllerTest {

    private MockMvc mvc;
    private RankingService service;
    private AutorizacaoPontuacao autorizacao;

    @BeforeEach
    void configurar() {
        service = mock(RankingService.class);
        autorizacao = mock(AutorizacaoPontuacao.class);

        RankingController controller =
            new RankingController(service, autorizacao);

        mvc = MockMvcBuilders.standaloneSetup(controller)
            .build();
    }

    private CicloRanking ciclo() {
        return CicloRanking.builder()
            .id("2026-10-05_7d")
            .inicio(OffsetDateTime.parse(
                "2026-10-05T00:00:00-03:00"))
            .fim(OffsetDateTime.parse(
                "2026-10-12T00:00:00-03:00"))
            .build();
    }

    @Test
    void deveConsultarRankingDeMoradores() throws Exception {
        when(autorizacao.podeConsultarCondominio(
            any(), eq(7)
        )).thenReturn(true);

        Ranking ranking = Ranking.builder()
            .tipo(TipoRankingType.MORADORES)
            .condominioId(7)
            .ciclo(ciclo())
            .participantes(List.of(
                ParticipanteRanking.builder()
                    .posicao(1L)
                    .participanteId(42)
                    .nome("Ana")
                    .pontos(100L)
                    .build()
            ))
            .build();

        when(service.consultar(
            TipoRankingType.MORADORES, 7, 10
        )).thenReturn(ranking);

        mvc.perform(get("/api/v1/rankings/moradores")
                .param("condominioId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo")
                .value("MORADORES"))
            .andExpect(jsonPath("$.condominioId")
                .value(7))
            .andExpect(jsonPath("$.ranking[0].usuarioId")
                .value(42))
            .andExpect(jsonPath("$.ranking[0].nome")
                .value("Ana"))
            .andExpect(jsonPath("$.ranking[0].pontos")
                .value(100));

        verify(service).consultar(
            TipoRankingType.MORADORES, 7, 10
        );
    }

    @Test
    void deveConsultarRankingDeTorres() throws Exception {
        when(autorizacao.podeConsultarCondominio(
            any(), eq(7)
        )).thenReturn(true);

        Ranking ranking = Ranking.builder()
            .tipo(TipoRankingType.TORRES)
            .condominioId(7)
            .ciclo(ciclo())
            .participantes(List.of(
                ParticipanteRanking.builder()
                    .posicao(1L)
                    .participanteId(5)
                    .nome("Torre A")
                    .pontos(200L)
                    .build()
            ))
            .build();

        when(service.consultar(
            TipoRankingType.TORRES, 7, 10
        )).thenReturn(ranking);

        mvc.perform(get("/api/v1/rankings/torres")
                .param("condominioId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ranking[0].torreId")
                .value(5))
            .andExpect(jsonPath("$.ranking[0].nome")
                .value("Torre A"))
            .andExpect(jsonPath("$.ranking[0].pontos")
                .value(200));
    }

    @Test
    void deveRetornarRankingVazio() throws Exception {
        when(autorizacao.podeConsultarCondominio(
            any(), eq(7)
        )).thenReturn(true);

        when(service.consultar(
            TipoRankingType.MORADORES, 7, 10
        )).thenReturn(
            Ranking.builder()
                .tipo(TipoRankingType.MORADORES)
                .condominioId(7)
                .ciclo(ciclo())
                .participantes(List.of())
                .build()
        );

        mvc.perform(get("/api/v1/rankings/moradores")
                .param("condominioId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ranking").isArray())
            .andExpect(jsonPath("$.ranking").isEmpty());
    }

    @Test
    void deveConsultarPosicaoDoMorador() throws Exception {
        when(autorizacao.podeConsultarCondominio(
            any(), eq(7)
        )).thenReturn(true);

        when(service.consultarPosicao(
            TipoRankingType.MORADORES, 7, 42
        )).thenReturn(3L);

        when(service.consultarPontuacao(
            TipoRankingType.MORADORES, 7, 42
        )).thenReturn(125L);

        when(service.contarParticipantes(
            TipoRankingType.MORADORES, 7
        )).thenReturn(50L);

        mvc.perform(get(
                "/api/v1/rankings/moradores/42/posicao")
                .param("condominioId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuarioId")
                .value(42))
            .andExpect(jsonPath("$.posicao")
                .value(3))
            .andExpect(jsonPath("$.pontos")
                .value(125))
            .andExpect(jsonPath("$.totalParticipantes")
                .value(50));
    }

    @Test
    void deveConsultarPosicaoDaTorre() throws Exception {
        when(autorizacao.podeConsultarTorre(
            any(), eq(7), eq(5)
        )).thenReturn(true);

        when(service.consultarPosicao(
            TipoRankingType.TORRES, 7, 5
        )).thenReturn(2L);

        when(service.consultarPontuacao(
            TipoRankingType.TORRES, 7, 5
        )).thenReturn(200L);

        when(service.contarParticipantes(
            TipoRankingType.TORRES, 7
        )).thenReturn(8L);

        mvc.perform(get(
                "/api/v1/rankings/torres/5/posicao")
                .param("condominioId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.torreId")
                .value(5))
            .andExpect(jsonPath("$.posicao")
                .value(2))
            .andExpect(jsonPath("$.pontos")
                .value(200))
            .andExpect(jsonPath("$.totalParticipantes")
                .value(8));
    }

    @Test
    void deveExigirCondominioId() throws Exception {
        mvc.perform(get("/api/v1/rankings/moradores"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deveUsarLimiteInformado() throws Exception {
        when(autorizacao.podeConsultarCondominio(
            any(), eq(7)
        )).thenReturn(true);

        when(service.consultar(
            TipoRankingType.MORADORES, 7, 5
        )).thenReturn(
            Ranking.builder()
                .tipo(TipoRankingType.MORADORES)
                .condominioId(7)
                .ciclo(ciclo())
                .participantes(List.of())
                .build()
        );

        mvc.perform(get("/api/v1/rankings/moradores")
                .param("condominioId", "7")
                .param("limit", "5"))
            .andExpect(status().isOk());

        verify(service).consultar(
            TipoRankingType.MORADORES, 7, 5
        );
    }
}
