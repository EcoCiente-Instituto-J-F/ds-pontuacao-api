
package br.com.ecociente.pontuacao.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.ecociente.pontuacao.core.service.RedisAdminService;
import br.com.ecociente.pontuacao.core.service.RedisAdminService.DiagnosticoRedis;
import br.com.ecociente.pontuacao.core.service.RedisAdminService.ResultadoSincronizacao;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/redis")
@RequiredArgsConstructor
public class RedisAdminController {

  private final RedisAdminService service;

  @PostMapping("/sincronizar-pendentes")
  @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
  public ResponseEntity<ResultadoSincronizacao> sincronizarPendentes() {

    return ResponseEntity.ok(
        service.sincronizarPendentes());
  }

  @GetMapping("/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<DiagnosticoRedis> consultarStatus() {

    return ResponseEntity.ok(
        service.consultarStatus());
  }
}
