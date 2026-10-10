package br.com.ecociente.pontuacao.entrypoint.dto.response;

import java.util.List;

import br.com.ecociente.pontuacao.core.domain.TipoRankingType;

public record RankingResponse<T>(
    TipoRankingType tipo,
    Integer condominioId,
    CicloRankingResponse ciclo,
    List<T> ranking
) {
}