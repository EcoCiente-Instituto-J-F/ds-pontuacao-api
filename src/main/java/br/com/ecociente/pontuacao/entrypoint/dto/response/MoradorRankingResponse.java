package br.com.ecociente.pontuacao.entrypoint.dto.response;

public record MoradorRankingResponse(
    Long posicao,
    Integer usuarioId,
    String nome,
    Long pontos
) {
}