package br.com.ecociente.pontuacao.dataprovaider.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.pontuacao.dataprovaider.entity.PostagemEntity;

public interface PostagemRepository extends JpaRepository<PostagemEntity, Integer> {

        @Query("""
                        SELECT p.usuarioId
                        FROM PostagemEntity p
                        WHERE p.id = :id
                        """)
        Optional<Integer> buscarUsuarioId(@Param("id") Integer id);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT p
                        FROM PostagemEntity p
                        WHERE p.id = :id
                        """)
        Optional<PostagemEntity> buscarComBloqueio(@Param("id") Integer id);

        @Query(value = """
                        SELECT id_usuario
                        FROM tb_usuarios
                        WHERE id_usuario = :usuarioId
                        FOR UPDATE
                        """, nativeQuery = true)
        Optional<Integer> bloquearUsuario(
                        @Param("usuarioId") Integer usuarioId);

        @Query("""
                        SELECT p.id
                        FROM PostagemEntity p
                        WHERE p.dataLimiteAnalise <= :agora
                          AND p.resolvidoEm IS NULL
                        ORDER BY p.dataLimiteAnalise ASC
                        """)
        List<Integer> buscarPostagensVencidas(
                        @Param("agora") OffsetDateTime agora,
                        Pageable pageable);

        @Query(value = """
                        SELECT id_postagem
                        FROM tb_postagens
                        WHERE data_limite_analise <= now()
                          AND resolvido_em IS NULL
                        ORDER BY data_limite_analise, id_postagem
                        LIMIT :limite
                        """, nativeQuery = true)
        List<Integer> buscarPostagensVencidas(
                        @Param("limite") int limite);

        @Query("""
                        SELECT p.condominioId
                        FROM PostagemEntity p
                        WHERE p.id = :id
                        """)
        Optional<Integer> buscarCondominioId(
                        @Param("id") Integer id);

}