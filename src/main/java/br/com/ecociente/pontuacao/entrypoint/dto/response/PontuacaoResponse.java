package br.com.ecociente.pontuacao.entrypoint.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.ecociente.pontuacao.core.domain.OrigemPontuacaoType;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PontuacaoResponse(
        OrigemPontuacaoType origemTipo,
        Integer postagemId,
        Integer tentativaId,
        Integer usuarioId,
        Integer categoriaId,
        Integer pontosConcedidos,
        TipoMovimentacaoType tipoMovimentacao,
        Long movimentacaoId,
        String motivo,
        RedisSincronizacaoResponse redis
) {
}