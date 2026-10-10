package br.com.ecociente.pontuacao.core.domain;

import java.time.OffsetDateTime;

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
public class CicloRanking {

    private String id;
    private OffsetDateTime inicio;
    private OffsetDateTime fim;
}