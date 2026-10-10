package br.com.ecociente.pontuacao.core.gateway;

import java.util.Optional;

import br.com.ecociente.pontuacao.core.domain.TentativaQuiz;

public interface TentativaQuizGateway {

  Optional<TentativaQuiz> buscarComBloqueio(Integer tentativaId);
}