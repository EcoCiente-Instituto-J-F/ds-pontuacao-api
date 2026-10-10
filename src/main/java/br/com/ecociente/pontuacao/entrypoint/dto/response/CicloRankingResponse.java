package br.com.ecociente.pontuacao.entrypoint.dto.response;

import java.time.OffsetDateTime;

public record CicloRankingResponse(
    String id,
    OffsetDateTime inicio,
    OffsetDateTime fim
) {
}