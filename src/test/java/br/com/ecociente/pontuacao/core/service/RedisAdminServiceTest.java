
package br.com.ecociente.pontuacao.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;

class RedisAdminServiceTest {

  private SincronizacaoPontuacaoGateway gateway;
  private SincronizacaoPontuacaoService sincronizacaoService;
  private StringRedisTemplate redisTemplate;
  private RedisAdminService service;

  @BeforeEach
  void configurar() {
    gateway = mock(SincronizacaoPontuacaoGateway.class);
    sincronizacaoService = mock(SincronizacaoPontuacaoService.class);
    redisTemplate = mock(StringRedisTemplate.class);

    service = new RedisAdminService(
        gateway,
        sincronizacaoService,
        redisTemplate,
        100);
  }

  @Test
  void deveSincronizarMovimentacoesPendentes() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(1L, 2L));

    when(sincronizacaoService.sincronizar(1L))
        .thenReturn(true);

    when(sincronizacaoService.sincronizar(2L))
        .thenReturn(true);

    when(gateway.contarPendentes()).thenReturn(0L);

    var resultado = service.sincronizarPendentes();

    assertEquals(2, resultado.processadas());
    assertEquals(2, resultado.sincronizadas());
    assertEquals(0, resultado.falhas());
    assertEquals(0L, resultado.pendentesRestantes());

    verify(gateway, never()).registrarFalha(anyLong());
  }

  @Test
  void deveRegistrarFalhaDeSincronizacao() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(1L, 2L));

    when(sincronizacaoService.sincronizar(1L))
        .thenReturn(true);

    when(sincronizacaoService.sincronizar(2L))
        .thenThrow(new IllegalStateException("Redis indisponível"));

    when(gateway.contarPendentes()).thenReturn(1L);

    var resultado = service.sincronizarPendentes();

    assertEquals(2, resultado.processadas());
    assertEquals(1, resultado.sincronizadas());
    assertEquals(1, resultado.falhas());
    assertEquals(1L, resultado.pendentesRestantes());

    verify(gateway).registrarFalha(2L);
    verify(gateway, never()).registrarFalha(1L);
  }

  @Test
  void deveRetornarZeroQuandoNaoExistiremPendencias() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of());

    when(gateway.contarPendentes()).thenReturn(0L);

    var resultado = service.sincronizarPendentes();

    assertEquals(0, resultado.processadas());
    assertEquals(0, resultado.sincronizadas());
    assertEquals(0, resultado.falhas());

    verifyNoInteractions(sincronizacaoService);
  }

  @Test
  void deveInformarRedisDisponivel() {
    when(redisTemplate.execute(
        org.mockito.ArgumentMatchers
            .<RedisCallback<Boolean>>any()))
        .thenReturn(true);

    when(gateway.contarPendentes()).thenReturn(3L);

    var resultado = service.consultarStatus();

    assertTrue(resultado.redisDisponivel());
    assertEquals(3L, resultado.movimentacoesPendentes());
  }

  @Test
  void deveInformarRedisIndisponivel() {
    when(redisTemplate.execute(
        org.mockito.ArgumentMatchers
            .<RedisCallback<Boolean>>any()))
        .thenThrow(new IllegalStateException("Conexão recusada"));

    when(gateway.contarPendentes()).thenReturn(5L);

    var resultado = service.consultarStatus();

    assertFalse(resultado.redisDisponivel());
    assertEquals(5L, resultado.movimentacoesPendentes());
  }

  @Test
  void deveManterPendenciaQuandoBloqueioNaoForObtido() {
    when(gateway.buscarIdsPendentes(100))
        .thenReturn(List.of(1L));

    when(sincronizacaoService.sincronizar(1L))
        .thenReturn(false);

    when(gateway.contarPendentes()).thenReturn(1L);

    var resultado = service.sincronizarPendentes();

    assertEquals(0, resultado.sincronizadas());
    assertEquals(0, resultado.falhas());
    assertEquals(1L, resultado.pendentesRestantes());

    verify(gateway, never()).registrarFalha(anyLong());
  }
}
