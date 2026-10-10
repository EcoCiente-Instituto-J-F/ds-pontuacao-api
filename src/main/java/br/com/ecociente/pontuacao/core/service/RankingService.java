
package br.com.ecociente.pontuacao.core.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.ParticipanteRanking;
import br.com.ecociente.pontuacao.core.domain.Ranking;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;
import br.com.ecociente.pontuacao.core.gateway.ParticipanteRankingGateway;
import br.com.ecociente.pontuacao.core.gateway.RankingRedisGateway;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingRedisGateway rankingGateway;
    private final CycleService cycleService;
    private final ParticipanteRankingGateway participanteGateway;

    public Ranking consultar(
            TipoRankingType tipo,
            Integer condominioId,
            int limite) {

        CicloRanking ciclo = cycleService.atual(tipo);

        String chave = cycleService.montarChave(
            tipo,
            condominioId,
            ciclo
        );

        List<ParticipanteRanking> participantes =
            rankingGateway.buscarTop(chave, limite);

        for (ParticipanteRanking participante : participantes) {

            if (tipo == TipoRankingType.MORADORES) {

                participante.setNome(
                    participanteGateway.buscarNomeMorador(
                        participante.getParticipanteId()
                    ).orElse(null)
                );

            } else {

                participante.setNome(
                    participanteGateway.buscarNomeTorre(
                        participante.getParticipanteId(),
                        condominioId
                    ).orElse(null)
                );
            }
        }

        return Ranking.builder()
            .tipo(tipo)
            .condominioId(condominioId)
            .ciclo(ciclo)
            .participantes(participantes)
            .build();
    }

    public Long consultarPosicao(
            TipoRankingType tipo,
            Integer condominioId,
            Integer participanteId) {

        String chave = montarChave(tipo, condominioId);

        return rankingGateway.buscarPosicao(
            chave,
            participanteId
        );
    }

    public Long consultarPontuacao(
            TipoRankingType tipo,
            Integer condominioId,
            Integer participanteId) {

        String chave = montarChave(tipo, condominioId);

        return rankingGateway.buscarPontuacao(
            chave,
            participanteId
        );
    }

    public Long contarParticipantes(
            TipoRankingType tipo,
            Integer condominioId) {

        return rankingGateway.contarParticipantes(
            montarChave(tipo, condominioId)
        );
    }

    private String montarChave(
            TipoRankingType tipo,
            Integer condominioId) {

        return cycleService.montarChave(
            tipo,
            condominioId,
            cycleService.atual(tipo)
        );
    }
}
