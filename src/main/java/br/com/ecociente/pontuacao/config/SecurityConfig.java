package br.com.ecociente.pontuacao.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import br.com.ecociente.pontuacao.entrypoint.dto.ErrorResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.ValidationError;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectMapper objectMapper,
      JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/actuator/health",
                "/error")
            .permitAll()

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/pontuacoes/**")
            .hasAnyRole("SERVICE", "ADMIN")

            .requestMatchers(
                HttpMethod.GET,
                "/api/v1/pontuacoes/**")
            .authenticated()

            .requestMatchers(
                HttpMethod.GET,
                "/api/v1/rankings/**")
            .authenticated()

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/admin/redis/sincronizar-pendentes")
            .hasAnyRole("ADMIN", "SERVICE")

            .requestMatchers(
                "/api/v1/admin/**")
            .hasRole("ADMIN")

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/postagens/*/finalizar-validacao")
            .hasRole("ADMIN")

            .requestMatchers(
                HttpMethod.PATCH,
                "/api/v1/postagens/*/decisao")
            .hasAnyRole("ADMIN", "SYNDIC")

            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(
                    jwtAuthenticationConverter))
            .authenticationEntryPoint((request, response, ex) -> escreverErro(
                response,
                objectMapper,
                401,
                "NAO_AUTENTICADO",
                "Informe um token válido"))
            .accessDeniedHandler((request, response, ex) -> escreverErro(
                response,
                objectMapper,
                403,
                "ACESSO_NEGADO",
                "Você não possui permissão para esta operação")))
        .exceptionHandling(errors -> errors
            .authenticationEntryPoint((request, response, ex) -> escreverErro(
                response,
                objectMapper,
                401,
                "NAO_AUTENTICADO",
                "Informe um token válido"))
            .accessDeniedHandler((request, response, ex) -> escreverErro(
                response,
                objectMapper,
                403,
                "ACESSO_NEGADO",
                "Você não possui permissão para esta operação")))
        .build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("perfil");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return converter;
  }

  @Bean
  public JwtDecoder jwtDecoder(
      @Value("${app.security.jwt.secret}") String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

    if (bytes.length < 32) {
      throw new IllegalArgumentException(
          "O segredo JWT deve possuir pelo menos 32 bytes");
    }

    MacAlgorithm algoritmo;
    String algoritmoChave;

    if (bytes.length >= 64) {
      algoritmo = MacAlgorithm.HS512;
      algoritmoChave = "HmacSHA512";
    } else if (bytes.length >= 48) {
      algoritmo = MacAlgorithm.HS384;
      algoritmoChave = "HmacSHA384";
    } else {
      algoritmo = MacAlgorithm.HS256;
      algoritmoChave = "HmacSHA256";
    }

    var key = new SecretKeySpec(bytes, algoritmoChave);

    var decoder = NimbusJwtDecoder.withSecretKey(key)
        .macAlgorithm(algoritmo)
        .build();

    OAuth2TokenValidator<Jwt> exigirExpiracao = jwt -> jwt.getExpiresAt() != null
        ? OAuth2TokenValidatorResult.success()
        : OAuth2TokenValidatorResult.failure(
            new OAuth2Error(
                "invalid_token",
                "O token deve possuir expiração",
                null));

    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(),
            exigirExpiracao));

    return decoder;
  }

  private static void escreverErro(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      int status,
      String codigo,
      String mensagem) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    if (status == 401) {
      response.setHeader("WWW-Authenticate", "Bearer");
    }

    var erro = new ErrorResponse(
        status,
        codigo,
        List.of(new ValidationError(null, mensagem)));

    objectMapper.writeValue(response.getOutputStream(), erro);
  }
}