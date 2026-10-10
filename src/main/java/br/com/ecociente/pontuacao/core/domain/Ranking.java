package br.com.ecociente.pontuacao.core.domain;

import java.util.List;

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
public class Ranking {

    private TipoRankingType tipo;
    private Integer condominioId;
    private CicloRanking ciclo;
    private List<ParticipanteRanking> participantes;
}