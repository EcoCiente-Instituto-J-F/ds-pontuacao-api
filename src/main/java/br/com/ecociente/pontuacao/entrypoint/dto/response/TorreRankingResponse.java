package br.com.ecociente.pontuacao.entrypoint.dto.response;

public record TorreRankingResponse(
    Long posicao,
    Integer torreId,
    String nome,
    Long pontos
) {
}