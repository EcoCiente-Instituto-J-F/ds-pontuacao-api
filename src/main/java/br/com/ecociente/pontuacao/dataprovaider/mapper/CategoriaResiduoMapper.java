package br.com.ecociente.pontuacao.dataprovaider.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.domain.CategoriaResiduo;
import br.com.ecociente.pontuacao.dataprovaider.entity.CategoriaResiduoEntity;

@Component
public class CategoriaResiduoMapper {

    public CategoriaResiduo toDomain(CategoriaResiduoEntity entity) {
        return CategoriaResiduo.builder()
                .id(entity.getId())
                .nomeCategoria(entity.getNomeCategoria())
                .pontosBase(entity.getPontosBase())
                .limitePontosDiario(entity.getLimitePontosDiario())
                .build();
    }
}