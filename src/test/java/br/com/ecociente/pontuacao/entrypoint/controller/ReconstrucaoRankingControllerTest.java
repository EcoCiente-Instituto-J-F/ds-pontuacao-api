
package br.com.ecociente.pontuacao.entrypoint.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import br.com.ecociente.pontuacao.core.service.ReconstrucaoRankingService;
import br.com.ecociente.pontuacao.core.service.ReconstrucaoRankingService.ResultadoReconstrucao;
import br.com.ecociente.pontuacao.entrypoint.dto.request.EscopoReconstrucaoType;

class ReconstrucaoRankingControllerTest {

    private MockMvc mvc;
    private ReconstrucaoRankingService service;

    @BeforeEach
    void configurar() {
        service = mock(ReconstrucaoRankingService.class);

        LocalValidatorFactoryBean validator =
            new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mvc = MockMvcBuilders.standaloneSetup(
            new ReconstrucaoRankingController(service)
        ).setValidator(validator).build();
    }

    @Test
    void deveReconstruirTodosOsRankings() throws Exception {
        when(service.reconstruir(
            EscopoReconstrucaoType.TODOS, 7
        )).thenReturn(
            new ResultadoReconstrucao("TODOS", 7, 20)
        );

        mvc.perform(post("/api/v1/admin/rankings/reconstruir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "TODOS",
                      "condominioId": 7,
                      "ciclo": "ATUAL"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo")
                .value("TODOS"))
            .andExpect(jsonPath("$.condominioId")
                .value(7))
            .andExpect(jsonPath("$.participantes")
                .value(20));

        verify(service).reconstruir(
            EscopoReconstrucaoType.TODOS, 7
        );
    }

    @Test
    void deveReconstruirSomenteMoradores() throws Exception {
        when(service.reconstruir(
            EscopoReconstrucaoType.MORADORES, 7
        )).thenReturn(
            new ResultadoReconstrucao("MORADORES", 7, 10)
        );

        mvc.perform(post("/api/v1/admin/rankings/reconstruir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "MORADORES",
                      "condominioId": 7,
                      "ciclo": "ATUAL"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantes")
                .value(10));

        verify(service).reconstruir(
            EscopoReconstrucaoType.MORADORES, 7
        );
    }

    @Test
    void deveRejeitarCicloNaoSuportado() throws Exception {
        mvc.perform(post("/api/v1/admin/rankings/reconstruir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "TODOS",
                      "condominioId": 7,
                      "ciclo": "ANTERIOR"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarCondominioZero() throws Exception {
        mvc.perform(post("/api/v1/admin/rankings/reconstruir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "TODOS",
                      "condominioId": 0,
                      "ciclo": "ATUAL"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarTipoAusente() throws Exception {
        mvc.perform(post("/api/v1/admin/rankings/reconstruir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "condominioId": 7,
                      "ciclo": "ATUAL"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarBodyInvalido() throws Exception {
        mvc.perform(post("/api/v1/admin/rankings/reconstruir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{json-invalido"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
