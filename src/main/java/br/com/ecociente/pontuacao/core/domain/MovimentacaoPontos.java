package br.com.ecociente.pontuacao.core.domain;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoPontos {

    private Long id;
    private Integer usuarioId;
    private Integer condominioId;
    private Integer torreId;
    private Integer categoriaId;
    private OrigemPontuacaoType origemTipo;
    private Integer postagemId;
    private Integer tentativaQuizId;
    private TipoMovimentacaoType tipoMovimentacao;
    private Integer pontos;
    private Long movimentacaoReferenciaId;
    private String idempotencyKey;
    private OffsetDateTime ocorridoEm;
    private Boolean redisSincronizado;
    private OffsetDateTime redisSincronizadoEm;
    private Integer tentativasSyncRedis;
    private String ultimoErroRedis;
}