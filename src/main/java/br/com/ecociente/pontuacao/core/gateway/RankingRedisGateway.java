
package br.com.ecociente.pontuacao.core.gateway;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.ecociente.pontuacao.core.domain.ParticipanteRanking;

public interface RankingRedisGateway {

  void definirPontuacao(
      String chave,
      Integer participanteId,
      Long pontos,
      OffsetDateTime expiraEm);

  List<ParticipanteRanking> buscarTop(
      String chave,
      int limite);

  Long buscarPosicao(
      String chave,
      Integer participanteId);

  Long buscarPontuacao(
      String chave,
      Integer participanteId);

  Long contarParticipantes(String chave);
}
