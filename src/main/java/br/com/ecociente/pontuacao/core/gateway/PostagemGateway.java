package br.com.ecociente.pontuacao.core.gateway;

import java.util.Optional;

import br.com.ecociente.pontuacao.core.domain.Postagem;

public interface PostagemGateway {

  Optional<Integer> buscarUsuarioId(Integer postagemId);

  boolean bloquearUsuario(Integer usuarioId);

  Optional<Postagem> buscarComBloqueio(Integer postagemId);

  void marcarReconciliacaoConcluida(Integer postagemId);

  Optional<Integer> buscarCondominioId(Integer postagemId);
}