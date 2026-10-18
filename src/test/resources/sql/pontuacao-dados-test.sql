INSERT INTO tb_lkp_tipos_usuarios (
    id_tipo_usuario, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Morador'),
    (2, 'Síndico');

INSERT INTO tb_lkp_tipos_condominios (
    id_tipo_condominio, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Residencial');

INSERT INTO tb_lkp_niveis_confianca (
    id_nivel_confianca, nome_nivel, peso_voto
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'morador_comum', 1),
    (2, 'pessoa_confiavel', 3),
    (3, 'sindico', 3);

INSERT INTO tb_lkp_status_validacoes_postagens (
    id_status_validacao, nome_status
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'aprovada'),
    (2, 'em_analise'),
    (3, 'reprovada');

INSERT INTO tb_lkp_tipos_votos_postagens (
    id_tipo_voto, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'aprovar'),
    (2, 'denunciar');

INSERT INTO tb_lkp_categorias_residuos (
    id_categoria, nome_categoria, pontos_base, limite_pontos_diario
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Papel', 10, 25);

INSERT INTO tb_enderecos (
    id_endereco, cep, estado, cidade, logradouro, numero
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '01001000', 'SP', 'São Paulo', 'Rua Um', '1'),
    (2, '01001000', 'SP', 'São Paulo', 'Rua Dois', '2');

INSERT INTO tb_usuarios (
    id_usuario, nome_usuario, email_usuario, senha_hash,
    ativo, tipo_usuario_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Morador Um', 'morador1@teste.com', 'hash', TRUE, 1, 1),
    (2, 'Síndico Um', 'sindico@teste.com', 'hash', TRUE, 2, 1),
    (3, 'Morador Outro', 'morador3@teste.com', 'hash', TRUE, 1, 2);

INSERT INTO tb_sindicos (id_sindico, usuario_id)
OVERRIDING SYSTEM VALUE
VALUES (1, 2);

INSERT INTO tb_condominios (
    id_condominio, nome_condominio, tipo_condominio_id,
    sindico_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Condomínio Um', 1, 1, 1),
    (2, 'Condomínio Dois', 1, NULL, 2);

INSERT INTO tb_torres (id_torre, nome_torre, condominio_id)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Torre A', 1),
    (2, 'Torre B', 1),
    (3, 'Torre Outro Condomínio', 2);

INSERT INTO tb_moradores (id_morador, usuario_id, condominio_id)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 1),
    (2, 3, 2);

INSERT INTO tb_rel_usuarios_condominios (
    usuario_id, condominio_id, aprovado,
    nivel_confianca_id, trust_score,
    postagens_validadas_sem_contestacao,
    denuncias_realizadas, denuncias_procedentes
)
VALUES
    (1, 1, TRUE, 1, 0, 0, 0, 0),
    (3, 2, TRUE, 1, 0, 0, 0, 0);

INSERT INTO tb_cursos (id_curso, titulo_curso)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Reciclagem');

INSERT INTO tb_aulas (
    id_aula, curso_id, titulo_aula, conteudo_aula, ordem
)
OVERRIDING SYSTEM VALUE
VALUES (1, 1, 'Introdução', 'Conteúdo', 1);

INSERT INTO tb_quizzes (
    id_quiz, curso_id, aula_id, titulo_quiz,
    nota_minima_aprovacao, pontos_recompensa, ativo
)
OVERRIDING SYSTEM VALUE
VALUES (1, 1, 1, 'Quiz', 70, 30, TRUE);