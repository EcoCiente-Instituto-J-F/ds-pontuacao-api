package br.com.ecociente.pontuacao.dataprovaider.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.pontuacao.dataprovaider.entity.TentativaQuizEntity;

public interface TentativaQuizRepository
    extends JpaRepository<TentativaQuizEntity, Integer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT t
      FROM TentativaQuizEntity t
      WHERE t.id = :id
      """)
  Optional<TentativaQuizEntity> buscarComBloqueio(
      @Param("id") Integer id);
}