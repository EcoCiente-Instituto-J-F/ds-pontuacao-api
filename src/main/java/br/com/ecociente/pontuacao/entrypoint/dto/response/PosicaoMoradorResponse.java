package br.com.ecociente.pontuacao.entrypoint.dto.response;

public record PosicaoMoradorResponse(
    Integer usuarioId,
    Integer condominioId,
    Long posicao,
    Long pontos,
    Long totalParticipantes
) {
}