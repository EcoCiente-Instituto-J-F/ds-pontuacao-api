
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

  @Override
  public List<Integer> buscarVencidasApos(
      Integer ultimoId,
      int limite) {
    return repository.buscarPostagensVencidasApos(
        ultimoId,
        limite);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean tentarBloquearFechamento(Integer postagemId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "SELECT pg_try_advisory_xact_lock(731942002, ?)",
            Boolean.class,
            postagemId));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean janelaVencida(Integer postagemId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            """
                SELECT EXISTS (
                    SELECT 1
                    FROM tb_postagens
                    WHERE id_postagem = ?
                      AND data_limite_analise <= now()
                )
                """,
            Boolean.class,
            postagemId));
  }
}
