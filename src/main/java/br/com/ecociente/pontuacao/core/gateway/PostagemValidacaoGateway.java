
package br.com.ecociente.pontuacao.core.gateway;

import java.util.List;

public interface PostagemValidacaoGateway {

  List<Integer> buscarVencidas(int limite);

  void encerrarJanela(Integer postagemId);

  void decidir(Integer postagemId, boolean aprovar);

  void atualizarTrustScore(
      Integer usuarioId,
      Integer condominioId);
}
