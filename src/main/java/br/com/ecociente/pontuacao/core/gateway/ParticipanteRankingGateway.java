package br.com.ecociente.pontuacao.core.gateway;

import java.util.Optional;

public interface ParticipanteRankingGateway {

  Optional<String> buscarNomeMorador(Integer usuarioId);

  Optional<String> buscarNomeTorre(
      Integer torreId,
      Integer condominioId);
}