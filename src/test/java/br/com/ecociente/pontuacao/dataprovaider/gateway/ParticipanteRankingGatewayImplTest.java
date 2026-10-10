
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ParticipanteRankingGatewayImplTest {

  private JdbcTemplate jdbcTemplate;
  private ParticipanteRankingGatewayImpl gateway;

  @BeforeEach
  void configurar() {
    jdbcTemplate = mock(JdbcTemplate.class);
    gateway = new ParticipanteRankingGatewayImpl(jdbcTemplate);
  }

  @Test
  void deveEncontrarNomeMorador() {
    when(jdbcTemplate.queryForList(
        anyString(), eq(String.class), eq(42))).thenReturn(List.of("Ana"));

    var nome = gateway.buscarNomeMorador(42);

    assertTrue(nome.isPresent());
    assertEquals("Ana", nome.get());
  }

  @Test
  void deveRetornarVazioParaMoradorInexistente() {
    when(jdbcTemplate.queryForList(
        anyString(), eq(String.class), eq(999))).thenReturn(List.of());

    assertTrue(gateway.buscarNomeMorador(999).isEmpty());
  }

  @Test
  void deveEncontrarNomeTorre() {
    when(jdbcTemplate.queryForList(
        anyString(), eq(String.class), eq(5), eq(7))).thenReturn(List.of("Torre A"));

    var nome = gateway.buscarNomeTorre(5, 7);

    assertEquals("Torre A", nome.orElseThrow());
  }

  @Test
  void deveRetornarVazioParaTorreDeOutroCondominio() {
    when(jdbcTemplate.queryForList(
        anyString(), eq(String.class), eq(5), eq(8))).thenReturn(List.of());

    assertTrue(gateway.buscarNomeTorre(5, 8).isEmpty());
  }
}
