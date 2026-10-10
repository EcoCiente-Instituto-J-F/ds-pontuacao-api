package br.com.ecociente.pontuacao.dataprovaider.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;
import br.com.ecociente.pontuacao.dataprovaider.entity.MovimentacaoPontosEntity;

import java.time.OffsetDateTime;
import java.util.List;

public interface MovimentacaoPontosRepository
        extends JpaRepository<MovimentacaoPontosEntity, Long> {

    long countByRedisSincronizadoFalse();

    Optional<MovimentacaoPontosEntity> findByIdempotencyKey(
            String idempotencyKey);

    Optional<MovimentacaoPontosEntity> findByPostagemIdAndTipoMovimentacao(
            Integer postagemId,
            TipoMovimentacaoType tipoMovimentacao);

    Optional<MovimentacaoPontosEntity> findByTentativaQuizIdAndTipoMovimentacao(
            Integer tentativaQuizId,
            TipoMovimentacaoType tipoMovimentacao);

    Optional<MovimentacaoPontosEntity> findFirstByPostagemIdOrderByIdDesc(
            Integer postagemId);

    Page<MovimentacaoPontosEntity> findByUsuarioId(
            Integer usuarioId,
            Pageable pageable);

    @Query(value = """
            SELECT COALESCE(
                SUM(
                    CASE
                        WHEN tipo_movimentacao = 'ESTORNO'
                            THEN -CAST(pontos AS BIGINT)
                        ELSE CAST(pontos AS BIGINT)
                    END
                ),
                0
            )
            FROM tb_movimentacoes_pontos
            WHERE usuario_id = :usuarioId
            """, nativeQuery = true)
    Long calcularSaldoUsuario(@Param("usuarioId") Integer usuarioId);

    @Query(value = """
            SELECT COALESCE(
                SUM(
                    CASE
                        WHEN tipo_movimentacao = 'ESTORNO'
                            THEN -CAST(pontos AS BIGINT)
                        ELSE CAST(pontos AS BIGINT)
                    END
                ),
                0
            )
            FROM tb_movimentacoes_pontos
            WHERE postagem_id = :postagemId
            """, nativeQuery = true)
    Long calcularSaldoPostagem(@Param("postagemId") Integer postagemId);

    List<MovimentacaoPontosEntity> findByRedisSincronizadoFalseOrderByIdAsc(
            Pageable pageable);

    @Query(value = """
            SELECT pg_try_advisory_xact_lock(731942001)
            """, nativeQuery = true)
    boolean tentarBloquearSincronizacao();

    @Query(value = """
            SELECT CAST(
                COALESCE(
                    SUM(
                        CASE
                            WHEN m.tipo_movimentacao = 'ESTORNO'
                                THEN -CAST(m.pontos AS BIGINT)
                            ELSE CAST(m.pontos AS BIGINT)
                        END
                    ),
                    0
                )
                AS BIGINT
            )
            FROM tb_movimentacoes_pontos m
            JOIN tb_movimentacoes_pontos credito
              ON credito.id_movimentacao =
                 COALESCE(m.movimentacao_referencia_id, m.id_movimentacao)
            WHERE credito.tipo_movimentacao = 'CREDITO'
              AND credito.usuario_id = :usuarioId
              AND credito.condominio_id = :condominioId
              AND credito.ocorrido_em >= :inicio
              AND credito.ocorrido_em < :fim
            """, nativeQuery = true)
    Long calcularSaldoMoradorNoCiclo(
            @Param("usuarioId") Integer usuarioId,
            @Param("condominioId") Integer condominioId,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

    @Query(value = """
            SELECT CAST(
                COALESCE(
                    SUM(
                        CASE
                            WHEN m.tipo_movimentacao = 'ESTORNO'
                                THEN -CAST(m.pontos AS BIGINT)
                            ELSE CAST(m.pontos AS BIGINT)
                        END
                    ),
                    0
                )
                AS BIGINT
            )
            FROM tb_movimentacoes_pontos m
            JOIN tb_movimentacoes_pontos credito
              ON credito.id_movimentacao =
                 COALESCE(m.movimentacao_referencia_id, m.id_movimentacao)
            WHERE credito.tipo_movimentacao = 'CREDITO'
              AND credito.torre_id = :torreId
              AND credito.condominio_id = :condominioId
              AND credito.ocorrido_em >= :inicio
              AND credito.ocorrido_em < :fim
            """, nativeQuery = true)
    Long calcularSaldoTorreNoCiclo(
            @Param("torreId") Integer torreId,
            @Param("condominioId") Integer condominioId,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

    @Query(value = """
            SELECT
                credito.usuario_id,
                CAST(SUM(
                    CASE
                        WHEN m.tipo_movimentacao = 'ESTORNO'
                            THEN -CAST(m.pontos AS BIGINT)
                        ELSE CAST(m.pontos AS BIGINT)
                    END
                ) AS BIGINT)
            FROM tb_movimentacoes_pontos m
            JOIN tb_movimentacoes_pontos credito
                ON credito.id_movimentacao =
                    COALESCE(
                        m.movimentacao_referencia_id,
                        m.id_movimentacao
                    )
            WHERE credito.tipo_movimentacao = 'CREDITO'
              AND credito.condominio_id = :condominioId
              AND credito.ocorrido_em >= :inicio
              AND credito.ocorrido_em < :fim
            GROUP BY credito.usuario_id
            """, nativeQuery = true)
    List<Object[]> agruparMoradoresNoCiclo(
            @Param("condominioId") Integer condominioId,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

    @Query(value = """
            SELECT
                credito.torre_id,
                CAST(SUM(
                    CASE
                        WHEN m.tipo_movimentacao = 'ESTORNO'
                            THEN -CAST(m.pontos AS BIGINT)
                        ELSE CAST(m.pontos AS BIGINT)
                    END
                ) AS BIGINT)
            FROM tb_movimentacoes_pontos m
            JOIN tb_movimentacoes_pontos credito
                ON credito.id_movimentacao =
                    COALESCE(
                        m.movimentacao_referencia_id,
                        m.id_movimentacao
                    )
            WHERE credito.tipo_movimentacao = 'CREDITO'
              AND credito.condominio_id = :condominioId
              AND credito.torre_id IS NOT NULL
              AND credito.ocorrido_em >= :inicio
              AND credito.ocorrido_em < :fim
            GROUP BY credito.torre_id
            """, nativeQuery = true)
    List<Object[]> agruparTorresNoCiclo(
            @Param("condominioId") Integer condominioId,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

}