
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.gateway.ParticipanteRankingGateway;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParticipanteRankingGatewayImpl
    implements ParticipanteRankingGateway {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public Optional<String> buscarNomeMorador(Integer usuarioId) {

    List<String> nomes = jdbcTemplate.queryForList(
        """
            SELECT nome_usuario
            FROM tb_usuarios
            WHERE id_usuario = ?
            """,
        String.class,
        usuarioId);

    return nomes.stream().findFirst();
  }

  @Override
  public Optional<String> buscarNomeTorre(
      Integer torreId,
      Integer condominioId) {

    List<String> nomes = jdbcTemplate.queryForList(
        """
            SELECT nome_torre
            FROM tb_torres
            WHERE id_torre = ?
              AND condominio_id = ?
            """,
        String.class,
        torreId,
        condominioId);

    return nomes.stream().findFirst();
  }
}
