package br.com.ecociente.pontuacao.dataprovaider.gateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.CicloRanking;
import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;
import br.com.ecociente.pontuacao.dataprovaider.mapper.MovimentacaoPontosMapper;
import br.com.ecociente.pontuacao.dataprovaider.repository.MovimentacaoPontosRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SincronizacaoPontuacaoGatewayImpl
                implements SincronizacaoPontuacaoGateway {

        private final MovimentacaoPontosRepository repository;
        private final MovimentacaoPontosMapper mapper;

        @Override
        public List<Long> buscarIdsPendentes(int limite) {
                return repository
                                .findByRedisSincronizadoFalseOrderByIdAsc(
                                                PageRequest.of(0, limite))
                                .stream()
                                .map(entity -> entity.getId())
                                .toList();
        }

        @Override
        @Transactional(propagation = Propagation.MANDATORY)
        public boolean tentarBloquearSincronizacao() {
                return repository.tentarBloquearSincronizacao();
        }

        @Override
        public Optional<MovimentacaoPontos> buscarPorId(Long movimentacaoId) {
                return repository.findById(movimentacaoId)
                                .map(mapper::toDomain);
        }

        @Override
        public Long calcularSaldoMorador(
                        Integer usuarioId,
                        Integer condominioId,
                        CicloRanking ciclo) {
                return repository.calcularSaldoMoradorNoCiclo(
                                usuarioId,
                                condominioId,
                                ciclo.getInicio(),
                                ciclo.getFim());
        }

        @Override
        public Long calcularSaldoTorre(
                        Integer torreId,
                        Integer condominioId,
                        CicloRanking ciclo) {
                return repository.calcularSaldoTorreNoCiclo(
                                torreId,
                                condominioId,
                                ciclo.getInicio(),
                                ciclo.getFim());
        }

        @Override
        @Transactional(propagation = Propagation.MANDATORY)
        public void marcarSincronizada(
                        Long movimentacaoId,
                        OffsetDateTime sincronizadoEm) {
                var entity = repository.findById(movimentacaoId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Movimentação não encontrada: " + movimentacaoId));

                entity.setRedisSincronizado(true);
                entity.setRedisSincronizadoEm(sincronizadoEm);
                entity.setUltimoErroRedis(null);

                repository.saveAndFlush(entity);
        }

        @Override
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void registrarFalha(Long movimentacaoId) {
                if (!repository.tentarBloquearSincronizacao()) {
                        return;
                }

                repository.findById(movimentacaoId).ifPresent(entity -> {
                        if (!Boolean.TRUE.equals(entity.getRedisSincronizado())) {
                                entity.setTentativasSyncRedis(
                                                entity.getTentativasSyncRedis() + 1);

                                entity.setUltimoErroRedis(
                                                "Falha na sincronização. Consulte os logs da aplicação.");

                                repository.saveAndFlush(entity);
                        }
                });
        }

        @Override
        public Long contarPendentes() {
                return repository.countByRedisSincronizadoFalse();
        }
}