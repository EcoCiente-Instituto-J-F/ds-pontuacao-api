
package br.com.ecociente.pontuacao.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.core.gateway.PostagemGateway;
import br.com.ecociente.pontuacao.core.service.PostagemValidacaoService;
import br.com.ecociente.pontuacao.entrypoint.dto.request.DecisaoPostagemRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;

import br.com.ecociente.pontuacao.config.AutorizacaoPontuacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/postagens")
@RequiredArgsConstructor
public class PostagemValidacaoController {

  private final PostagemValidacaoService service;
  private final PostagemGateway postagemGateway;
  private final AutorizacaoPontuacao autorizacaoPontuacao;

  @PostMapping("/{postagemId}/finalizar-validacao")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> finalizar(
      @PathVariable @Positive Integer postagemId) {

    service.encerrar(postagemId);

    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{postagemId}/decisao")
  @PreAuthorize("hasAnyRole('ADMIN', 'SYNDIC')")
  public ResponseEntity<Void> decidir(
      @PathVariable @Positive Integer postagemId,
      @Valid @RequestBody DecisaoPostagemRequest request,
      Authentication authentication) {

    Integer condominioId = postagemGateway
        .buscarCondominioId(postagemId)
        .orElseThrow(() -> new NotFoundException(
            "POSTAGEM_NAO_ENCONTRADA",
            "Postagem não encontrada"));

    if (!autorizacaoPontuacao.podeDecidirPostagem(
        authentication,
        condominioId)) {
      throw new AccessDeniedException(
          "Você não pode decidir postagens deste condomínio");
    }

    service.decidir(postagemId, request.aprovar());

    return ResponseEntity.ok().build();
  }

}
