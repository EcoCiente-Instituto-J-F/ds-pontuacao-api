package br.com.ecociente.pontuacao.entrypoint.dto.response;

public record SaldoPontuacaoResponse(
    Integer usuarioId,
    Long saldo
) {
}