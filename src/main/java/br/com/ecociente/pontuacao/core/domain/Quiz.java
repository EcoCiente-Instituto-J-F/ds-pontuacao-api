package br.com.ecociente.pontuacao.core.domain;

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
public class Quiz {

    private Integer id;
    private String tituloQuiz;
    private Integer pontosRecompensa;
    private Boolean ativo;
}