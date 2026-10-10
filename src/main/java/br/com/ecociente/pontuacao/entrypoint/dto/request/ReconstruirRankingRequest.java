package br.com.ecociente.pontuacao.entrypoint.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para reconstruir os rankings a partir do PostgreSQL")
public record ReconstruirRankingRequest(

        @Schema(description = "Tipo de ranking que será reconstruído", example = "TODOS", allowableValues = {
                "MORADORES", "TORRES",
                "TODOS" }) @NotNull(message = "O tipo de ranking é obrigatório") EscopoReconstrucaoType tipo,

        @Schema(description = "Identificador do condomínio", example = "7", minimum = "1") @NotNull(message = "O condomínio é obrigatório") Integer condominioId,

        @Schema(description = "Identificador do ciclo ou ATUAL para o ciclo vigente", example = "ATUAL", maxLength = 40) @NotBlank(message = "O ciclo é obrigatório") @Size(max = 40, message = "O ciclo deve possuir no máximo 40 caracteres") String ciclo

    ) {
}