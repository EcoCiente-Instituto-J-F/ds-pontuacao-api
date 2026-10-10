package br.com.ecociente.pontuacao.core.gateway;

import java.util.Optional;

import br.com.ecociente.pontuacao.core.domain.Quiz;

public interface QuizGateway {

  Optional<Quiz> buscarPorId(Integer quizId);
}