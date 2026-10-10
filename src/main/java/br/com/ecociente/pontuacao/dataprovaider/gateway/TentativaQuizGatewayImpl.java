package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.TentativaQuiz;
import br.com.ecociente.pontuacao.core.gateway.TentativaQuizGateway;
import br.com.ecociente.pontuacao.dataprovaider.mapper.TentativaQuizMapper;
import br.com.ecociente.pontuacao.dataprovaider.repository.TentativaQuizRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TentativaQuizGatewayImpl implements TentativaQuizGateway {

  private final TentativaQuizRepository repository;
  private final TentativaQuizMapper mapper;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<TentativaQuiz> buscarComBloqueio(Integer tentativaId) {
    return repository.buscarComBloqueio(tentativaId)
        .map(mapper::toDomain);
  }
}