package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.Postagem;
import br.com.ecociente.pontuacao.core.gateway.PostagemGateway;
import br.com.ecociente.pontuacao.dataprovaider.mapper.PostagemMapper;
import br.com.ecociente.pontuacao.dataprovaider.repository.PostagemRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostagemGatewayImpl implements PostagemGateway {

  private final PostagemRepository repository;
  private final PostagemMapper mapper;

  @Override
  public Optional<Integer> buscarUsuarioId(Integer postagemId) {
    return repository.buscarUsuarioId(postagemId);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean bloquearUsuario(Integer usuarioId) {
    return repository.bloquearUsuario(usuarioId).isPresent();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Postagem> buscarComBloqueio(Integer postagemId) {
    return repository.buscarComBloqueio(postagemId)
        .map(mapper::toDomain);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void marcarReconciliacaoConcluida(Integer postagemId) {
    var entity = repository.buscarComBloqueio(postagemId)
        .orElseThrow(() -> new IllegalStateException(
            "Postagem não encontrada ao concluir reconciliação: "
                + postagemId));

    entity.setPontuacaoReconciliacaoPendente(false);

    repository.saveAndFlush(entity);
  }

  @Override
  public Optional<Integer> buscarCondominioId(
      Integer postagemId) {

    return repository.buscarCondominioId(postagemId);
  }
}