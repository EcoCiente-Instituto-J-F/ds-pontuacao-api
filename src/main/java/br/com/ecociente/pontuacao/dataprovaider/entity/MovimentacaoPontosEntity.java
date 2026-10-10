package br.com.ecociente.pontuacao.dataprovaider.entity;

import java.time.OffsetDateTime;

import br.com.ecociente.pontuacao.core.domain.OrigemPontuacaoType;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_movimentacoes_pontos")
public class MovimentacaoPontosEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimentacao")
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Integer usuarioId;

    @Column(name = "condominio_id", updatable = false)
    private Integer condominioId;

    @Column(name = "torre_id", updatable = false)
    private Integer torreId;

    @Column(name = "categoria_id", updatable = false)
    private Integer categoriaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_tipo", nullable = false, length = 20, updatable = false)
    private OrigemPontuacaoType origemTipo;

    @Column(name = "postagem_id", updatable = false)
    private Integer postagemId;

    @Column(name = "tentativa_quiz_id", updatable = false)
    private Integer tentativaQuizId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimentacao", nullable = false, length = 20, updatable = false)
    private TipoMovimentacaoType tipoMovimentacao;

    @Column(name = "pontos", nullable = false, updatable = false)
    private Integer pontos;

    @Column(name = "movimentacao_referencia_id", updatable = false)
    private Long movimentacaoReferenciaId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 150, updatable = false)
    private String idempotencyKey;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private OffsetDateTime ocorridoEm;

    @Column(name = "redis_sincronizado", nullable = false)
    private Boolean redisSincronizado = false;

    @Column(name = "redis_sincronizado_em")
    private OffsetDateTime redisSincronizadoEm;

    @Column(name = "tentativas_sync_redis", nullable = false)
    private Integer tentativasSyncRedis = 0;

    @Column(name = "ultimo_erro_redis", columnDefinition = "TEXT")
    private String ultimoErroRedis;
}