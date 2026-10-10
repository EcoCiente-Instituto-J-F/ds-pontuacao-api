package br.com.ecociente.pontuacao.dataprovaider.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.domain.Quiz;
import br.com.ecociente.pontuacao.dataprovaider.entity.QuizEntity;

@Component
public class QuizMapper {

    public Quiz toDomain(QuizEntity entity) {
        return Quiz.builder()
                .id(entity.getId())
                .tituloQuiz(entity.getTituloQuiz())
                .pontosRecompensa(entity.getPontosRecompensa())
                .ativo(entity.getAtivo())
                .build();
    }
}