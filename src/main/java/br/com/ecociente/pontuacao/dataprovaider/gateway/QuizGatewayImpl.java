package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.Quiz;
import br.com.ecociente.pontuacao.core.gateway.QuizGateway;
import br.com.ecociente.pontuacao.dataprovaider.mapper.QuizMapper;
import br.com.ecociente.pontuacao.dataprovaider.repository.QuizRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizGatewayImpl implements QuizGateway {

  private final QuizRepository repository;
  private final QuizMapper mapper;

  @Override
  public Optional<Quiz> buscarPorId(Integer quizId) {
    return repository.findById(quizId)
        .map(mapper::toDomain);
  }
}