package br.com.ecociente.pontuacao.dataprovaider.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.domain.Postagem;
import br.com.ecociente.pontuacao.dataprovaider.entity.PostagemEntity;

@Component
public class PostagemMapper {

    public Postagem toDomain(PostagemEntity entity) {
        return Postagem.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .condominioId(entity.getCondominioId())
                .torreId(entity.getTorreId())
                .categoriaId(entity.getCategoriaId())
                .statusValidacaoId(entity.getStatusValidacaoId())
                .saldoConfianca(entity.getSaldoConfianca())
                .pontuacaoAtiva(entity.getPontuacaoAtiva())
                .pontuacaoReconciliacaoPendente(
                        entity.getPontuacaoReconciliacaoPendente())
                .dataPostagem(entity.getDataPostagem())
                .dataLimiteAnalise(entity.getDataLimiteAnalise())
                .resolvidoEm(entity.getResolvidoEm())
                .build();
    }
}