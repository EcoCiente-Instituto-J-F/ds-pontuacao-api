package br.com.ecociente.pontuacao.entrypoint.dto;

public record ValidationError(
    String field,
    String message) {
}