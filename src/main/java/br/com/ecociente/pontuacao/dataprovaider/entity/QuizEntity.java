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
@Table(name = "tb_quizzes")
public class QuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_quiz")
    private Integer id;

    @Column(name = "curso_id", nullable = false)
    private Integer cursoId;

    @Column(name = "aula_id", nullable = false, unique = true)
    private Integer aulaId;

    @Column(name = "titulo_quiz", nullable = false, length = 255)
    private String tituloQuiz;

    @Column(name = "nota_minima_aprovacao", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaMinimaAprovacao;

    @Column(name = "pontos_recompensa", nullable = false)
    private Integer pontosRecompensa;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;
}