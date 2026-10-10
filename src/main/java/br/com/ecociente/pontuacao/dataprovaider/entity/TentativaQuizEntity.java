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
@Table(name = "tb_tentativas_quiz")
public class TentativaQuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tentativa")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "quiz_id", nullable = false)
    private Integer quizId;

    @Column(name = "condominio_id")
    private Integer condominioId;

    @Column(name = "torre_id")
    private Integer torreId;

    @Column(name = "nota", precision = 5, scale = 2)
    private BigDecimal nota;

    @Column(name = "aprovado", nullable = false)
    private Boolean aprovado;

    @Column(name = "iniciado_em", nullable = false)
    private OffsetDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;
}