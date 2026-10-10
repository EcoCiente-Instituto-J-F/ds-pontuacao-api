package br.com.ecociente.pontuacao.entrypoint.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.ecociente.pontuacao.core.domain.AcaoReconciliacaoType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReconciliacaoPontuacaoResponse(
        Integer postagemId,
        AcaoReconciliacaoType acao,
        Integer pontos,
        Long movimentacaoId,
        RedisSincronizacaoResponse redis
) {
}