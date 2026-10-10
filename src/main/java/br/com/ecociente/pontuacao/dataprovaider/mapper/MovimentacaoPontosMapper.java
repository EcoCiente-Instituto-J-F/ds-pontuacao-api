package br.com.ecociente.pontuacao.dataprovaider.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.dataprovaider.entity.MovimentacaoPontosEntity;

@Component
public class MovimentacaoPontosMapper {

    public MovimentacaoPontos toDomain(MovimentacaoPontosEntity entity) {
        return MovimentacaoPontos.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .condominioId(entity.getCondominioId())
                .torreId(entity.getTorreId())
                .categoriaId(entity.getCategoriaId())
                .origemTipo(entity.getOrigemTipo())
                .postagemId(entity.getPostagemId())
                .tentativaQuizId(entity.getTentativaQuizId())
                .tipoMovimentacao(entity.getTipoMovimentacao())
                .pontos(entity.getPontos())
                .movimentacaoReferenciaId(entity.getMovimentacaoReferenciaId())
                .idempotencyKey(entity.getIdempotencyKey())
                .ocorridoEm(entity.getOcorridoEm())
                .redisSincronizado(entity.getRedisSincronizado())
                .redisSincronizadoEm(entity.getRedisSincronizadoEm())
                .tentativasSyncRedis(entity.getTentativasSyncRedis())
                .ultimoErroRedis(entity.getUltimoErroRedis())
                .build();
    }

    public MovimentacaoPontosEntity toEntity(MovimentacaoPontos domain) {
        MovimentacaoPontosEntity entity = new MovimentacaoPontosEntity();

        entity.setId(domain.getId());
        entity.setUsuarioId(domain.getUsuarioId());
        entity.setCondominioId(domain.getCondominioId());
        entity.setTorreId(domain.getTorreId());
        entity.setCategoriaId(domain.getCategoriaId());
        entity.setOrigemTipo(domain.getOrigemTipo());
        entity.setPostagemId(domain.getPostagemId());
        entity.setTentativaQuizId(domain.getTentativaQuizId());
        entity.setTipoMovimentacao(domain.getTipoMovimentacao());
        entity.setPontos(domain.getPontos());
        entity.setMovimentacaoReferenciaId(domain.getMovimentacaoReferenciaId());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setOcorridoEm(domain.getOcorridoEm());

        entity.setRedisSincronizado(
                Boolean.TRUE.equals(domain.getRedisSincronizado()));

        entity.setRedisSincronizadoEm(domain.getRedisSincronizadoEm());

        entity.setTentativasSyncRedis(
                domain.getTentativasSyncRedis() == null
                        ? 0
                        : domain.getTentativasSyncRedis());

        entity.setUltimoErroRedis(domain.getUltimoErroRedis());

        return entity;
    }
}