
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class VotoPostagemGatewayImplTest {

  private JdbcTemplate jdbcTemplate;
  private VotoPostagemGatewayImpl gateway;

  @BeforeEach
  void configurar() {
    jdbcTemplate = mock(JdbcTemplate.class);
    gateway = new VotoPostagemGatewayImpl(jdbcTemplate);
  }

  @Test
  void deveRegistrarVotoDeAprovacao() {
    gateway.registrar(10, 42, "aprovar", null, null);

    verify(jdbcTemplate).update(
        "CALL sp_processar_voto_postagem(?, ?, ?, ?, ?)",
        10, 42, "aprovar", null, null);
  }

  @Test
  void deveRegistrarDenunciaComMotivo() {
    gateway.registrar(
        10, 42, "denunciar", 3, "Foto reutilizada");

    verify(jdbcTemplate).update(
        "CALL sp_processar_voto_postagem(?, ?, ?, ?, ?)",
        10, 42, "denunciar", 3, "Foto reutilizada");
  }

  @Test
  void devePropagarErroDeVotoDuplicado() {
    when(jdbcTemplate.update(
        "CALL sp_processar_voto_postagem(?, ?, ?, ?, ?)",
        10, 42, "aprovar", null, null)).thenThrow(new IllegalStateException("Voto duplicado"));

    assertThrows(
        IllegalStateException.class,
        () -> gateway.registrar(
            10, 42, "aprovar", null, null));
  }
}
