
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.gateway.PostagemValidacaoGateway;
import br.com.ecociente.pontuacao.dataprovaider.repository.PostagemRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostagemValidacaoGatewayImpl
    implements PostagemValidacaoGateway {

  private final PostagemRepository repository;
  private final JdbcTemplate jdbcTemplate;

  @Override
  public List<Integer> buscarVencidas(int limite) {
    return repository.buscarPostagensVencidas(limite);
  }

  @Override
  public void encerrarJanela(Integer postagemId) {
    jdbcTemplate.update(
        "CALL sp_encerrar_janela_postagem(?)",
        postagemId);
  }

  @Override
  public void decidir(
      Integer postagemId,
      boolean aprovar) {

    jdbcTemplate.update(
        "CALL sp_decidir_postagem_analise(?, ?)",
        postagemId,
        aprovar);
  }

  @Override
  public void atualizarTrustScore(
      Integer usuarioId,
      Integer condominioId) {

    jdbcTemplate.update(
        "CALL sp_atualizar_trust_score(?, ?)",
        usuarioId,
        condominioId);
  }
}
