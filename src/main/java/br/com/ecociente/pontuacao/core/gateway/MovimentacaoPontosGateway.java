package br.com.ecociente.pontuacao.core.gateway;

import java.util.Optional;

import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.Pagina;

public interface MovimentacaoPontosGateway {

  MovimentacaoPontos criar(MovimentacaoPontos movimentacao);

  Optional<MovimentacaoPontos> buscarPorIdempotencyKey(
      String idempotencyKey);

  Optional<MovimentacaoPontos> buscarCreditoPostagem(
      Integer postagemId);

  Optional<MovimentacaoPontos> buscarCreditoTentativa(
      Integer tentativaId);

  Optional<MovimentacaoPontos> buscarUltimaMovimentacaoPostagem(
      Integer postagemId);

  Long calcularSaldoUsuario(Integer usuarioId);

  Long calcularSaldoPostagem(Integer postagemId);

  Pagina<MovimentacaoPontos> listarPorUsuario(
      Integer usuarioId,
      int pagina,
      int tamanho);
}