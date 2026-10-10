package br.com.ecociente.pontuacao.dataprovaider.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_lkp_categorias_residuos")
public class CategoriaResiduoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer id;

    @Column(name = "nome_categoria", nullable = false, length = 100)
    private String nomeCategoria;

    @Column(name = "descricao_material", length = 255)
    private String descricaoMaterial;

    @Column(name = "permite_reciclagem", nullable = false)
    private Boolean permiteReciclagem;

    @Column(name = "cor_identificacao", length = 10)
    private String corIdentificacao;

    @Column(name = "pontos_base", nullable = false)
    private Integer pontosBase;

    @Column(name = "limite_pontos_diario")
    private Integer limitePontosDiario;
}