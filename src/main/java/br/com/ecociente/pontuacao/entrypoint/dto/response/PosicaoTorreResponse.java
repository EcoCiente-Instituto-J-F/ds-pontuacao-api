package br.com.ecociente.pontuacao.entrypoint.dto.response;

public record PosicaoTorreResponse(
    Integer torreId,
    Integer condominioId,
    Long posicao,
    Long pontos,
    Long totalParticipantes
) {
}