package br.com.ecociente.pontuacao.entrypoint.dto;

import java.util.List;

public record ErrorResponse(
    int status,
    String codigoError,
    List<ValidationError> details) {
}