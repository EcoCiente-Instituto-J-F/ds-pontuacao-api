package br.com.ecociente.pontuacao.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.Pagina;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.core.gateway.MovimentacaoPontosGateway;

@ExtendWith(MockitoExtension.class)
class ConsultaPontuacaoServiceTest {

  @Mock
  private MovimentacaoPontosGateway movimentacaoGateway;

  @InjectMocks
  private ConsultaPontuacaoService service;

  @ParameterizedTest
  @ValueSource(longs = { 0L, 125L })
  @DisplayName("Deve retornar o saldo consultado no histórico oficial")
  void shouldReturnOfficialBalance(long balance) {
    when(movimentacaoGateway.calcularSaldoUsuario(42))
        .thenReturn(balance);

    Long resultado = service.consultarSaldo(42);

    assertEquals(balance, resultado.longValue());
    verify(movimentacaoGateway).calcularSaldoUsuario(42);
  }

  @Test
  @DisplayName("Deve rejeitar saldo negativo")
  void shouldRejectNegativeBalance() {
    when(movimentacaoGateway.calcularSaldoUsuario(42))
        .thenReturn(-10L);

    InconsistenciaPontuacaoException ex = assertThrows(
        InconsistenciaPontuacaoException.class,
        () -> service.consultarSaldo(42));

    assertEquals("INCONSISTENCIA_MOVIMENTACAO", ex.getCodigoErro());
  }

  @Test
  @DisplayName("Deve consultar o histórico com usuário e paginação informados")
  void shouldReturnMovementHistoryWithRequestedPagination() {
    MovimentacaoPontos movimentacao = MovimentacaoPontos.builder()
        .id(100L)
        .usuarioId(42)
        .pontos(10)
        .build();

    Pagina<MovimentacaoPontos> pagina = new Pagina<>(
        List.of(movimentacao),
        1,
        20,
        21L,
        2);

    when(movimentacaoGateway.listarPorUsuario(42, 1, 20))
        .thenReturn(pagina);

    Pagina<MovimentacaoPontos> resultado = service.listarMovimentacoes(42, 1, 20);

    assertSame(pagina, resultado);
    assertEquals(1, resultado.pagina());
    assertEquals(21L, resultado.totalElementos());

    verify(movimentacaoGateway).listarPorUsuario(42, 1, 20);
  }
}