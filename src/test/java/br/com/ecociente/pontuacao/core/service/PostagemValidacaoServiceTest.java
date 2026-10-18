
package br.com.ecociente.pontuacao.core.service;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.ecociente.pontuacao.core.domain.Postagem;
import br.com.ecociente.pontuacao.core.exception.BusinessException;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.core.gateway.PostagemGateway;
import br.com.ecociente.pontuacao.core.gateway.PostagemValidacaoGateway;

class PostagemValidacaoServiceTest {

    private PostagemValidacaoGateway validacaoGateway;
    private PostagemGateway postagemGateway;
    private PontuacaoService pontuacaoService;
    private PostagemValidacaoService service;

    @BeforeEach
    void configurar() {
        validacaoGateway = mock(PostagemValidacaoGateway.class);
        postagemGateway = mock(PostagemGateway.class);
        pontuacaoService = mock(PontuacaoService.class);

        service = new PostagemValidacaoService(
                validacaoGateway,
                postagemGateway,
                pontuacaoService);

        when(postagemGateway.buscarUsuarioId(10))
                .thenReturn(Optional.of(42));

        when(postagemGateway.bloquearUsuario(42))
                .thenReturn(true);

        when(postagemGateway.buscarComBloqueio(10))
                .thenReturn(Optional.of(postagem()));

        when(validacaoGateway.tentarBloquearFechamento(10))
                .thenReturn(true);

        when(validacaoGateway.janelaVencida(10))
                .thenReturn(true);
    }

    private Postagem postagem() {
        return Postagem.builder()
                .id(10)
                .usuarioId(42)
                .condominioId(7)
                .saldoConfianca(0)
                .pontuacaoAtiva(true)
                .build();
    }

    @Test
    void deveEncerrarEReconciliarPostagem() {
        service.encerrar(10);

        var ordem = inOrder(
                validacaoGateway,
                postagemGateway,
                pontuacaoService);

        ordem.verify(validacaoGateway).tentarBloquearFechamento(10);
        ordem.verify(postagemGateway).buscarUsuarioId(10);
        ordem.verify(postagemGateway).bloquearUsuario(42);
        ordem.verify(postagemGateway).buscarComBloqueio(10);
        ordem.verify(validacaoGateway).janelaVencida(10);
        ordem.verify(validacaoGateway).encerrarJanela(10);

        ordem.verify(pontuacaoService).reconciliarPostagem(
                10,
                "fechamento-postagem-10");

        ordem.verify(validacaoGateway).atualizarTrustScore(42, 7);
    }

    @Test
    void deveAprovarPostagemEmAnalise() {
        service.decidir(10, true);

        var ordem = inOrder(
                postagemGateway,
                validacaoGateway,
                pontuacaoService);

        ordem.verify(postagemGateway).buscarUsuarioId(10);
        ordem.verify(postagemGateway).bloquearUsuario(42);
        ordem.verify(postagemGateway).buscarComBloqueio(10);
        ordem.verify(validacaoGateway).decidir(10, true);

        ordem.verify(pontuacaoService).reconciliarPostagem(
                10,
                "decisao-postagem-10");

        ordem.verify(validacaoGateway).atualizarTrustScore(42, 7);
    }

    @Test
    void deveReprovarPostagemEmAnalise() {
        when(postagemGateway.buscarComBloqueio(10))
                .thenReturn(Optional.of(postagem()));

        service.decidir(10, false);

        verify(validacaoGateway).decidir(10, false);
        verify(pontuacaoService).reconciliarPostagem(
                10, "decisao-postagem-10");
    }

    @Test
    void naoDeveReconciliarSeProcedureFalhar() {
        doThrow(new IllegalStateException("Janela ainda aberta"))
                .when(validacaoGateway).encerrarJanela(10);

        assertThrows(IllegalStateException.class,
                () -> service.encerrar(10));

        verifyNoInteractions(pontuacaoService);
        verify(validacaoGateway, never())
                .atualizarTrustScore(anyInt(), anyInt());
    }

    @Test
    void deveFalharQuandoPostagemNaoForEncontrada() {
        when(postagemGateway.buscarComBloqueio(10))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.encerrar(10));

        verifyNoInteractions(pontuacaoService);
    }

    @Test
    void naoDeveAtualizarConfiancaSeReconciliacaoFalhar() {
        when(postagemGateway.buscarComBloqueio(10))
                .thenReturn(Optional.of(postagem()));

        when(pontuacaoService.reconciliarPostagem(
                eq(10), anyString())).thenThrow(new IllegalStateException(
                        "Falha na reconciliação"));

        assertThrows(IllegalStateException.class,
                () -> service.encerrar(10));

        verify(validacaoGateway, never())
                .atualizarTrustScore(anyInt(), anyInt());
    }

    @Test
    void naoDeveExecutarProcedureQuandoUsuarioNaoPuderSerBloqueado() {
        when(postagemGateway.bloquearUsuario(42))
                .thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> service.encerrar(10));

        verify(postagemGateway, never()).buscarComBloqueio(anyInt());

        verify(validacaoGateway, never()).encerrarJanela(anyInt());
        verify(validacaoGateway, never()).janelaVencida(anyInt());

        verify(validacaoGateway, never())
                .atualizarTrustScore(anyInt(), anyInt());

        verifyNoInteractions(pontuacaoService);

    }

    @Test
    void naoDeveProcessarQuandoOutraInstanciaPossuirLock() {
        when(validacaoGateway.tentarBloquearFechamento(10))
                .thenReturn(false);

        assertThrows(
                BusinessException.class,
                () -> service.encerrar(10));

        verifyNoInteractions(postagemGateway, pontuacaoService);
        verify(validacaoGateway, never()).encerrarJanela(anyInt());
    }

    @Test
    void naoDeveReprocessarPostagemJaEncerrada() {
        Postagem encerrada = postagem();

        encerrada.setResolvidoEm(
                java.time.OffsetDateTime.parse("2026-10-17T12:00:00Z"));

        when(postagemGateway.buscarComBloqueio(10))
                .thenReturn(Optional.of(encerrada));

        service.encerrar(10);

        verify(validacaoGateway, never()).janelaVencida(anyInt());
        verify(validacaoGateway, never()).encerrarJanela(anyInt());

        verify(validacaoGateway, never())
                .atualizarTrustScore(anyInt(), anyInt());

        verifyNoInteractions(pontuacaoService);
    }

    @Test
    void naoDeveEncerrarAntesDoVencimentoDaJanela() {
        when(validacaoGateway.janelaVencida(10))
                .thenReturn(false);

        assertThrows(
                BusinessException.class,
                () -> service.encerrar(10));

        verify(validacaoGateway, never()).encerrarJanela(anyInt());
        verifyNoInteractions(pontuacaoService);
    }
}
