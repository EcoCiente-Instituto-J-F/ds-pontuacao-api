package br.com.ecociente.pontuacao.entrypoint.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.ecociente.pontuacao.core.service.RedisAdminService;
import br.com.ecociente.pontuacao.core.service.RedisAdminService.DiagnosticoRedis;
import br.com.ecociente.pontuacao.core.service.RedisAdminService.ResultadoSincronizacao;

class RedisAdminControllerTest {

    private MockMvc mvc;
    private RedisAdminService service;

    @BeforeEach
    void configurar() {
        service = mock(RedisAdminService.class);

        mvc = MockMvcBuilders.standaloneSetup(
            new RedisAdminController(service)
        ).build();
    }

    @Test
    void deveSincronizarPendentes() throws Exception {
        when(service.sincronizarPendentes())
            .thenReturn(new ResultadoSincronizacao(
                5, 4, 1, 1L
            ));

        mvc.perform(post(
                "/api/v1/admin/redis/sincronizar-pendentes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processadas")
                .value(5))
            .andExpect(jsonPath("$.sincronizadas")
                .value(4))
            .andExpect(jsonPath("$.falhas")
                .value(1))
            .andExpect(jsonPath("$.pendentesRestantes")
                .value(1));

        verify(service).sincronizarPendentes();
    }

    @Test
    void deveRetornarResultadoSemPendencias() throws Exception {
        when(service.sincronizarPendentes())
            .thenReturn(new ResultadoSincronizacao(
                0, 0, 0, 0L
            ));

        mvc.perform(post(
                "/api/v1/admin/redis/sincronizar-pendentes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processadas")
                .value(0))
            .andExpect(jsonPath("$.pendentesRestantes")
                .value(0));
    }

    @Test
    void deveConsultarRedisDisponivel() throws Exception {
        when(service.consultarStatus())
            .thenReturn(new DiagnosticoRedis(true, 3L));

        mvc.perform(get("/api/v1/admin/redis/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.redisDisponivel")
                .value(true))
            .andExpect(jsonPath("$.movimentacoesPendentes")
                .value(3));
    }

    @Test
    void deveConsultarRedisIndisponivel() throws Exception {
        when(service.consultarStatus())
            .thenReturn(new DiagnosticoRedis(false, 10L));

        mvc.perform(get("/api/v1/admin/redis/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.redisDisponivel")
                .value(false))
            .andExpect(jsonPath("$.movimentacoesPendentes")
                .value(10));
    }
}
