
package br.com.ecociente.pontuacao.config.job;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.ecociente.pontuacao.core.gateway.PostagemValidacaoGateway;
import br.com.ecociente.pontuacao.core.service.PostagemValidacaoService;

class PostValidationClosingJobTest {

    private PostagemValidacaoGateway gateway;
    private PostagemValidacaoService service;
    private PostValidationClosingJob job;

    @BeforeEach
    void configurar() {
        gateway = mock(PostagemValidacaoGateway.class);
        service = mock(PostagemValidacaoService.class);

        job = new PostValidationClosingJob(
            gateway,
            service,
            100
        );
    }

    @Test
    void deveEncerrarTodasAsPostagensVencidas() {
        when(gateway.buscarVencidas(100))
            .thenReturn(List.of(10, 11, 12));

        job.executar();

        verify(service).encerrar(10);
        verify(service).encerrar(11);
        verify(service).encerrar(12);
        verify(service, times(3)).encerrar(anyInt());
    }

    @Test
    void naoDeveProcessarQuandoNaoExistiremPostagens() {
        when(gateway.buscarVencidas(100))
            .thenReturn(List.of());

        job.executar();

        verifyNoInteractions(service);
    }

    @Test
    void deveContinuarSeUmaPostagemFalhar() {
        when(gateway.buscarVencidas(100))
            .thenReturn(List.of(10, 11, 12));

        doThrow(new IllegalStateException("Falha na postagem"))
            .when(service).encerrar(11);

        assertDoesNotThrow(() -> job.executar());

        verify(service).encerrar(10);
        verify(service).encerrar(11);
        verify(service).encerrar(12);
    }

    @Test
    void deveRejeitarLoteInvalido() {
        assertThrows(IllegalArgumentException.class,
            () -> new PostValidationClosingJob(
                gateway, service, 0
            ));

        assertThrows(IllegalArgumentException.class,
            () -> new PostValidationClosingJob(
                gateway, service, 1001
            ));
    }
}
