
package br.com.ecociente.pontuacao.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.gateway.VinculoCondominialGateway;

import lombok.RequiredArgsConstructor;

@Component("autorizacaoPontuacao")
@RequiredArgsConstructor
public class AutorizacaoPontuacao {

  private final VinculoCondominialGateway vinculoGateway;

  public boolean podeConsultar(
      Authentication authentication,
      Integer usuarioId) {

    if (!autenticado(authentication)) {
      return false;
    }

    if (possuiPapel(authentication, "ROLE_ADMIN")) {
      return true;
    }

    Integer usuarioAutenticado = obterUsuarioId(authentication);

    return usuarioAutenticado != null
        && usuarioAutenticado.equals(usuarioId);
  }

  public boolean podeConsultarCondominio(
      Authentication authentication,
      Integer condominioId) {

    if (!autenticado(authentication)
        || condominioId == null) {
      return false;
    }

    if (possuiPapel(authentication, "ROLE_ADMIN")) {
      return true;
    }

    Integer usuarioId = obterUsuarioId(authentication);

    return usuarioId != null
        && vinculoGateway.possuiVinculoAtivo(
            usuarioId,
            condominioId);
  }

  public boolean podeConsultarTorre(
      Authentication authentication,
      Integer condominioId,
      Integer torreId) {

    return podeConsultarCondominio(
        authentication,
        condominioId)
        && vinculoGateway.torrePertenceCondominio(
            torreId,
            condominioId);
  }

  public boolean podeDecidirPostagem(
      Authentication authentication,
      Integer condominioId) {

    if (!autenticado(authentication)) {
      return false;
    }

    if (possuiPapel(authentication, "ROLE_ADMIN")) {
      return true;
    }

    if (!possuiPapel(authentication, "ROLE_SYNDIC")) {
      return false;
    }

    Integer usuarioId = obterUsuarioId(authentication);

    return usuarioId != null
        && vinculoGateway.ehSindico(
            usuarioId,
            condominioId);
  }

  private boolean autenticado(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated();
  }

  private boolean possuiPapel(
      Authentication authentication,
      String papel) {

    return authentication.getAuthorities()
        .stream()
        .anyMatch(authority -> papel.equals(authority.getAuthority()));
  }

  private Integer obterUsuarioId(Authentication authentication) {

    if (!(authentication instanceof JwtAuthenticationToken jwt)) {
      return null;
    }

    Object valor = jwt.getToken()
        .getClaims()
        .get("usuarioId");

    if (valor == null) {
      return null;
    }

    try {
      return Integer.valueOf(valor.toString());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
