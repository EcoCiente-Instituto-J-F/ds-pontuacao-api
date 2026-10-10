
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.gateway.VotoPostagemGateway;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VotoPostagemGatewayImpl
    implements VotoPostagemGateway {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void registrar(
      Integer postagemId,
      Integer usuarioId,
      String tipoVoto,
      Integer motivoDenunciaId,
      String comentario) {

    jdbcTemplate.update(
        "CALL sp_processar_voto_postagem(?, ?, ?, ?, ?)",
        postagemId,
        usuarioId,
        tipoVoto,
        motivoDenunciaId,
        comentario);
  }
}
