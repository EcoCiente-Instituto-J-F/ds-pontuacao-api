package br.com.ecociente.pontuacao.entrypoint.dto.request;

import jakarta.validation.constraints.NotNull;

public record DecisaoPostagemRequest(
    @NotNull(message = "A decisão é obrigatória") Boolean aprovar) {
}
