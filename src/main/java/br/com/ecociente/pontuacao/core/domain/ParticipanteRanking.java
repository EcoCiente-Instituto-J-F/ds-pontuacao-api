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
public class ParticipanteRanking {

    private Long posicao;
    private Integer participanteId;
    private String nome;
    private Long pontos;
}