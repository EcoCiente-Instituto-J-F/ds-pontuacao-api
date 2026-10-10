
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.ecociente.pontuacao.dataprovaider.repository.PostagemRepository;

class PostagemValidacaoGatewayImplTest {

    private PostagemRepository repository;
    private JdbcTemplate jdbcTemplate;
    private PostagemValidacaoGatewayImpl gateway;

    @BeforeEach
    void configurar() {
        repository = mock(PostagemRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        gateway = new PostagemValidacaoGatewayImpl(
            repository,
            jdbcTemplate
        );
    }

    @Test
    void deveBuscarPostagensVencidas() {
        when(repository.buscarPostagensVencidas(100))
            .thenReturn(List.of(10, 11, 12));

        List<Integer> resultado = gateway.buscarVencidas(100);

        assertEquals(List.of(10, 11, 12), resultado);

        verify(repository).buscarPostagensVencidas(100);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistiremPostagensVencidas() {
        when(repository.buscarPostagensVencidas(100))
            .thenReturn(List.of());

        List<Integer> resultado = gateway.buscarVencidas(100);

        assertTrue(resultado.isEmpty());

        verify(repository).buscarPostagensVencidas(100);
    }

    @Test
    void deveRespeitarLimiteDaBusca() {
        when(repository.buscarPostagensVencidas(5))
            .thenReturn(List.of(10, 11));

        List<Integer> resultado = gateway.buscarVencidas(5);

        assertEquals(2, resultado.size());
        verify(repository).buscarPostagensVencidas(5);
        verify(repository, never()).buscarPostagensVencidas(100);
    }

    @Test
    void deveExecutarProcedureDeFechamento() {
        gateway.encerrarJanela(10);

        verify(jdbcTemplate).update(
            "CALL sp_encerrar_janela_postagem(?)",
            10
        );

        verifyNoInteractions(repository);
    }

    @Test
    void deveExecutarProcedureDeAprovacao() {
        gateway.decidir(10, true);

        verify(jdbcTemplate).update(
            "CALL sp_decidir_postagem_analise(?, ?)",
            10,
            true
        );
    }

    @Test
    void deveExecutarProcedureDeReprovacao() {
        gateway.decidir(10, false);

        verify(jdbcTemplate).update(
            "CALL sp_decidir_postagem_analise(?, ?)",
            10,
            false
        );
    }

    @Test
    void deveAtualizarTrustScore() {
        gateway.atualizarTrustScore(42, 7);

        verify(jdbcTemplate).update(
            "CALL sp_atualizar_trust_score(?, ?)",
            42,
            7
        );
    }

    @Test
    void devePropagarErroAoEncerrarJanela() {
        doThrow(new IllegalStateException("Falha no banco"))
            .when(jdbcTemplate)
            .update(
                "CALL sp_encerrar_janela_postagem(?)",
                10
            );

        assertThrows(
            IllegalStateException.class,
            () -> gateway.encerrarJanela(10)
        );

        verify(jdbcTemplate).update(
            "CALL sp_encerrar_janela_postagem(?)",
            10
        );
    }

    @Test
    void devePropagarErroAoDecidirPostagem() {
        doThrow(new IllegalStateException("Decisao invalida"))
            .when(jdbcTemplate)
            .update(
                "CALL sp_decidir_postagem_analise(?, ?)",
                10,
                true
            );

        assertThrows(
            IllegalStateException.class,
            () -> gateway.decidir(10, true)
        );
    }

    @Test
    void devePropagarErroAoAtualizarTrustScore() {
        doThrow(new IllegalStateException("Falha SQL"))
            .when(jdbcTemplate)
            .update(
                "CALL sp_atualizar_trust_score(?, ?)",
                42,
                7
            );

        assertThrows(
            IllegalStateException.class,
            () -> gateway.atualizarTrustScore(42, 7)
        );
    }

    @Test
    void devePropagarErroAoBuscarPostagens() {
        when(repository.buscarPostagensVencidas(100))
            .thenThrow(new IllegalStateException("Erro de consulta"));

        assertThrows(
            IllegalStateException.class,
            () -> gateway.buscarVencidas(100)
        );
    }
}
