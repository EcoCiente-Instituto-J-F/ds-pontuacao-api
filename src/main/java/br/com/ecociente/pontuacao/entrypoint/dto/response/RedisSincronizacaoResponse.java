package br.com.ecociente.pontuacao.entrypoint.dto.response;

public record RedisSincronizacaoResponse(
    boolean sincronizado,
    String status
) {
}