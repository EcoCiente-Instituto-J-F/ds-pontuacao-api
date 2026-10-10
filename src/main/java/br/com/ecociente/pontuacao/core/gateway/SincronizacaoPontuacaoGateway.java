package br.com.ecociente.pontuacao.core.gateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;

public interface SincronizacaoPontuacaoGateway {

        List<Long> buscarIdsPendentes(int limite);

        boolean tentarBloquearSincronizacao();

        Optional<MovimentacaoPontos> buscarPorId(Long movimentacaoId);

        Long calcularSaldoMorador(
                        Integer usuarioId,
                        Integer condominioId,
                        CicloRanking ciclo);

        Long calcularSaldoTorre(
                        Integer torreId,
                        Integer condominioId,
                        CicloRanking ciclo);

        void marcarSincronizada(
                        Long movimentacaoId,
                        OffsetDateTime sincronizadoEm);

        void registrarFalha(Long movimentacaoId);

        Long contarPendentes();
}