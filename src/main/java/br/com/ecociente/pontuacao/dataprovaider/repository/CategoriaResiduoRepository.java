package br.com.ecociente.pontuacao.dataprovaider.repository;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.pontuacao.dataprovaider.entity.CategoriaResiduoEntity;

public interface CategoriaResiduoRepository
                extends JpaRepository<CategoriaResiduoEntity, Integer> {

        @Query(value = """
                        SELECT fn_pontos_disponiveis_postagem(
                            :usuarioId,
                            :categoriaId,
                            CAST(:dataReferencia AS TIMESTAMPTZ),
                            CAST(:timezone AS TEXT)
                        )
                        """, nativeQuery = true)
        Integer calcularPontosDisponiveis(
                        @Param("usuarioId") Integer usuarioId,
                        @Param("categoriaId") Integer categoriaId,
                        @Param("dataReferencia") OffsetDateTime dataReferencia,
                        @Param("timezone") String timezone);
}