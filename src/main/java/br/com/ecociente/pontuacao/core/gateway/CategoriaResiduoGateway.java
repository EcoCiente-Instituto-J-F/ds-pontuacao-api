package br.com.ecociente.pontuacao.core.gateway;

import java.time.OffsetDateTime;
import java.util.Optional;

import br.com.ecociente.pontuacao.core.domain.CategoriaResiduo;

public interface CategoriaResiduoGateway {

  Optional<CategoriaResiduo> buscarPorId(Integer categoriaId);

  Integer calcularPontosDisponiveis(
      Integer usuarioId,
      Integer categoriaId,
      OffsetDateTime dataReferencia,
      String timezone);
}