
package br.com.ecociente.pontuacao.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.ecociente.pontuacao.core.service.ReconstrucaoRankingService;
import br.com.ecociente.pontuacao.core.service.ReconstrucaoRankingService.ResultadoReconstrucao;
import br.com.ecociente.pontuacao.entrypoint.dto.request.ReconstruirRankingRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/rankings")
@RequiredArgsConstructor
public class ReconstrucaoRankingController {

  private final ReconstrucaoRankingService service;

  @PostMapping("/reconstruir")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ResultadoReconstrucao> reconstruir(
      @Valid @RequestBody ReconstruirRankingRequest request) {

    if (!"ATUAL".equalsIgnoreCase(request.ciclo())) {
      return ResponseEntity.badRequest().build();
    }

    if (request.condominioId() <= 0) {
      return ResponseEntity.badRequest().build();
    }

    return ResponseEntity.ok(
        service.reconstruir(
            request.tipo(),
            request.condominioId()));
  }
}
