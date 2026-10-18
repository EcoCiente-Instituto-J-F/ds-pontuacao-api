package br.com.ecociente.pontuacao.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.pontuacao.core.service.ReconstrucaoRankingService;
import br.com.ecociente.pontuacao.core.service.ReconstrucaoRankingService.ResultadoReconstrucao;
import br.com.ecociente.pontuacao.entrypoint.dto.request.ReconstruirRankingRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/rankings")
public class ReconstrucaoRankingController {

    private final ReconstrucaoRankingService service;

    public ReconstrucaoRankingController(
        ReconstrucaoRankingService service
    ) {
        this.service = service;
    }

    @PostMapping("/reconstruir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResultadoReconstrucao> reconstruir(
        @Valid @RequestBody ReconstruirRankingRequest request
    ) {
        return ResponseEntity.ok(
            service.reconstruir(
                request.tipo(),
                request.condominioId()
            )
        );
    }
}