package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.CategoriaResiduo;
import br.com.ecociente.pontuacao.core.gateway.CategoriaResiduoGateway;
import br.com.ecociente.pontuacao.dataprovaider.mapper.CategoriaResiduoMapper;
import br.com.ecociente.pontuacao.dataprovaider.repository.CategoriaResiduoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaResiduoGatewayImpl implements CategoriaResiduoGateway {

  private final CategoriaResiduoRepository repository;
  private final CategoriaResiduoMapper mapper;

  @Override
  public Optional<CategoriaResiduo> buscarPorId(Integer categoriaId) {
    return repository.findById(categoriaId)
        .map(mapper::toDomain);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Integer calcularPontosDisponiveis(
      Integer usuarioId,
      Integer categoriaId,
      OffsetDateTime dataReferencia,
      String timezone) {
    return repository.calcularPontosDisponiveis(
        usuarioId,
        categoriaId,
        dataReferencia,
        timezone);
  }
}