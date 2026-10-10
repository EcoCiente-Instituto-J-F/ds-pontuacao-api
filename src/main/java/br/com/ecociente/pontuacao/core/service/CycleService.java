package br.com.ecociente.pontuacao.core.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;

@Service
public class CycleService {

        private final Clock clock;
        private final LocalDate marcoInicial;

        public CycleService(
                        Clock clock,
                        @Value("${app.ranking.cycle-epoch}") String marcoInicial) {
                this.clock = clock;
                this.marcoInicial = LocalDate.parse(marcoInicial);
        }

        public CicloRanking atual(TipoRankingType tipo) {
                return calcular(OffsetDateTime.now(clock), tipo);
        }

        public CicloRanking calcular(
                        OffsetDateTime referencia,
                        TipoRankingType tipo) {
                int duracao = tipo == TipoRankingType.MORADORES ? 7 : 30;

                LocalDate data = referencia
                                .atZoneSameInstant(clock.getZone())
                                .toLocalDate();

                long diasDesdeMarco = ChronoUnit.DAYS.between(marcoInicial, data);
                long numeroCiclo = Math.floorDiv(diasDesdeMarco, duracao);

                LocalDate inicio = marcoInicial.plusDays(numeroCiclo * duracao);
                LocalDate fim = inicio.plusDays(duracao);

                return CicloRanking.builder()
                                .id(inicio + "_" + duracao + "d")
                                .inicio(inicio.atStartOfDay(clock.getZone()).toOffsetDateTime())
                                .fim(fim.atStartOfDay(clock.getZone()).toOffsetDateTime())
                                .build();
        }

        public String montarChave(
                        TipoRankingType tipo,
                        Integer condominioId,
                        CicloRanking ciclo) {
                String nome = tipo == TipoRankingType.MORADORES
                                ? "moradores"
                                : "torres";

                return "ecociente:ranking:"
                                + nome + ":"
                                + condominioId + ":"
                                + ciclo.getId();
        }
}