package br.com.ecociente.pontuacao.entrypoint.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VotoPostagemRequest(
    @NotBlank String tipoVoto,
    Integer motivoDenunciaId,
    String comentario) {
}