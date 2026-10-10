package br.com.ecociente.pontuacao.entrypoint.dto.response;

import java.time.OffsetDateTime;

import br.com.ecociente.pontuacao.core.domain.OrigemPontuacaoType;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;

public record MovimentacaoPontosResponse(
    Long movimentacaoId,
    Integer usuarioId,
    Integer condominioId,
    Integer torreId,
    Integer categoriaId,
    OrigemPontuacaoType origemTipo,
    Integer postagemId,
    Integer tentativaQuizId,
    TipoMovimentacaoType tipoMovimentacao,
    Integer pontos,
    Long movimentacaoReferenciaId,
    OffsetDateTime ocorridoEm
) {
}