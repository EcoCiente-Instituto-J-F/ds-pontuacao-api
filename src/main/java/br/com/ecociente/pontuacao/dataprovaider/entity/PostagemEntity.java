package br.com.ecociente.pontuacao.dataprovaider.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_postagens")
public class PostagemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postagem")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "condominio_id", nullable = false)
    private Integer condominioId;

    @Column(name = "torre_id")
    private Integer torreId;

    @Column(name = "categoria_id", nullable = false)
    private Integer categoriaId;

    @Column(name = "url_foto", nullable = false, length = 500)
    private String urlFoto;

    @Column(name = "hash_foto", nullable = false, unique = true, length = 128)
    private String hashFoto;

    @Column(name = "capturada_em", nullable = false)
    private OffsetDateTime capturadaEm;

    @Column(name = "data_postagem", nullable = false)
    private OffsetDateTime dataPostagem;

    @Column(name = "status_validacao_id", nullable = false)
    private Integer statusValidacaoId;

    @Column(name = "saldo_confianca", nullable = false)
    private Integer saldoConfianca;

    @Column(name = "pontuacao_ativa", nullable = false)
    private Boolean pontuacaoAtiva;

    @Column(name = "pontuacao_reconciliacao_pendente", nullable = false)
    private Boolean pontuacaoReconciliacaoPendente;

    @Column(name = "triagem_automatica_aprovada")
    private Boolean triagemAutomaticaAprovada;

    @Column(name = "triagem_automatica_confianca", precision = 5, scale = 2)
    private BigDecimal triagemAutomaticaConfianca;

    @Column(name = "data_limite_analise", nullable = false)
    private OffsetDateTime dataLimiteAnalise;

    @Column(name = "resolvido_em")
    private OffsetDateTime resolvidoEm;
}