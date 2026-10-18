package br.com.ecociente.pontuacao.entrypoint.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para reconstruir os rankings a partir do PostgreSQL")
public record ReconstruirRankingRequest(

    @NotNull(message = "O tipo de ranking é obrigatório")
    @Schema(
        description = "Tipo de ranking",
        example = "TODOS"
    )
    EscopoReconstrucaoType tipo,

    @NotNull(message = "O condomínio é obrigatório")
    @Positive(message = "O condomínio deve possuir ID positivo")
    @Schema(example = "7")
    Integer condominioId,

    @NotBlank(message = "O ciclo é obrigatório")
    @Size(
        max = 40,
        message = "O ciclo deve possuir no máximo 40 caracteres"
    )
    @Pattern(
        regexp = "(?i)ATUAL",
        message = "Somente o ciclo ATUAL pode ser reconstruído"
    )
    @Schema(
        description = "Ciclo vigente",
        example = "ATUAL",
        allowableValues = {"ATUAL"}
    )
    String ciclo

) {
}