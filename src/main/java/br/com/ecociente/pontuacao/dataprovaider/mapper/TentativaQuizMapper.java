package br.com.ecociente.pontuacao.dataprovaider.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.domain.TentativaQuiz;
import br.com.ecociente.pontuacao.dataprovaider.entity.TentativaQuizEntity;

@Component
public class TentativaQuizMapper {

    public TentativaQuiz toDomain(TentativaQuizEntity entity) {
        return TentativaQuiz.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .quizId(entity.getQuizId())
                .condominioId(entity.getCondominioId())
                .torreId(entity.getTorreId())
                .nota(entity.getNota())
                .aprovado(entity.getAprovado())
                .iniciadoEm(entity.getIniciadoEm())
                .concluidoEm(entity.getConcluidoEm())
                .build();
    }
}