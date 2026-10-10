
package br.com.ecociente.pontuacao.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.ecociente.pontuacao.core.gateway.VinculoCondominialGateway;

class AutorizacaoPontuacaoTest {

  private VinculoCondominialGateway gateway;
  private AutorizacaoPontuacao autorizacao;

  @BeforeEach
  void configurar() {
    gateway = mock(VinculoCondominialGateway.class);
    autorizacao = new AutorizacaoPontuacao(gateway);
  }

  private JwtAuthenticationToken usuario(
      Integer usuarioId,
      String papel) {

    Jwt jwt = Jwt.withTokenValue("token-teste")
        .header("alg", "none")
        .claim("usuarioId", usuarioId)
        .build();

    return new JwtAuthenticationToken(
        jwt,
        List.of(new SimpleGrantedAuthority(papel)));
  }

  @Test
  void usuarioPodeConsultarSeusMovimentos() {
    assertTrue(autorizacao.podeConsultar(
        usuario(42, "ROLE_USER"), 42));
  }

  @Test
  void usuarioNaoPodeConsultarMovimentosDeOutro() {
    assertFalse(autorizacao.podeConsultar(
        usuario(42, "ROLE_USER"), 99));
  }

  @Test
  void adminPodeConsultarMovimentosDeOutro() {
    assertTrue(autorizacao.podeConsultar(
        usuario(1, "ROLE_ADMIN"), 42));
  }

  @Test
  void usuarioPodeConsultarCondominioComVinculoAtivo() {
    when(gateway.possuiVinculoAtivo(42, 7))
        .thenReturn(true);

    assertTrue(autorizacao.podeConsultarCondominio(
        usuario(42, "ROLE_USER"), 7));
  }

  @Test
  void usuarioNaoPodeConsultarOutroCondominio() {
    when(gateway.possuiVinculoAtivo(42, 8))
        .thenReturn(false);

    assertFalse(autorizacao.podeConsultarCondominio(
        usuario(42, "ROLE_USER"), 8));
  }

  @Test
  void adminPodeConsultarCondominio() {
    assertTrue(autorizacao.podeConsultarCondominio(
        usuario(1, "ROLE_ADMIN"), 7));
  }

  @Test
  void deveNegarTorreDeOutroCondominio() {
    when(gateway.possuiVinculoAtivo(42, 7))
        .thenReturn(true);

    when(gateway.torrePertenceCondominio(5, 7))
        .thenReturn(false);

    assertFalse(autorizacao.podeConsultarTorre(
        usuario(42, "ROLE_USER"), 7, 5));
  }

  @Test
  void sindicoPodeDecidirNoProprioCondominio() {
    when(gateway.ehSindico(42, 7))
        .thenReturn(true);

    assertTrue(autorizacao.podeDecidirPostagem(
        usuario(42, "ROLE_SYNDIC"), 7));
  }

  @Test
  void sindicoNaoPodeDecidirEmOutroCondominio() {
    when(gateway.ehSindico(42, 8))
        .thenReturn(false);

    assertFalse(autorizacao.podeDecidirPostagem(
        usuario(42, "ROLE_SYNDIC"), 8));
  }

  @Test
  void moradorNaoPodeDecidirPostagem() {
    assertFalse(autorizacao.podeDecidirPostagem(
        usuario(42, "ROLE_USER"), 7));
  }

  @Test
  void deveNegarUsuarioNaoAutenticado() {
    assertFalse(autorizacao.podeConsultarCondominio(
        null, 7));
  }

  @Test
  void deveNegarAutenticacaoSemJwt() {
    var authentication = new UsernamePasswordAuthenticationToken(
        "usuario",
        "senha",
        List.of(new SimpleGrantedAuthority("ROLE_USER")));

    assertFalse(autorizacao.podeConsultarCondominio(
        authentication, 7));
  }
}
