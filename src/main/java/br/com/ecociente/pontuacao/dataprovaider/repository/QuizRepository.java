package br.com.ecociente.pontuacao.dataprovaider.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.pontuacao.dataprovaider.entity.QuizEntity;

public interface QuizRepository extends JpaRepository<QuizEntity, Integer> {
}