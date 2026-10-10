package br.com.ecociente.pontuacao.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.Pagina;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.core.gateway.MovimentacaoPontosGateway;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaPontuacaoService {

  private final MovimentacaoPontosGateway movimentacaoGateway;

  public Long consultarSaldo(Integer usuarioId) {
    Long saldo = movimentacaoGateway.calcularSaldoUsuario(usuarioId);

    if (saldo < 0) {
      throw new InconsistenciaPontuacaoException(
          "INCONSISTENCIA_MOVIMENTACAO",
          "O histórico do usuário possui saldo negativo");
    }

    return saldo;
  }

  public Pagina<MovimentacaoPontos> listarMovimentacoes(
      Integer usuarioId,
      int pagina,
      int tamanho) {
    return movimentacaoGateway.listarPorUsuario(
        usuarioId,
        pagina,
        tamanho);
  }
}