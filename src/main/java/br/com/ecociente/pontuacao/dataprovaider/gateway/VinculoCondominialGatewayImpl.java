
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.gateway.VinculoCondominialGateway;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VinculoCondominialGatewayImpl
        implements VinculoCondominialGateway {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean possuiVinculoAtivo(
            Integer usuarioId,
            Integer condominioId) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_rel_usuarios_condominios
                WHERE usuario_id = ?
                  AND condominio_id = ?
                  AND aprovado = TRUE
                  AND data_saida IS NULL
            )
            """;

        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                usuarioId,
                condominioId
            )
        );
    }

    @Override
    public boolean torrePertenceCondominio(
            Integer torreId,
            Integer condominioId) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_torres
                WHERE id_torre = ?
                  AND condominio_id = ?
            )
            """;

        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                torreId,
                condominioId
            )
        );
    }

    @Override
    public boolean ehSindico(
            Integer usuarioId,
            Integer condominioId) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_condominios c
                JOIN tb_sindicos s
                  ON s.id_sindico = c.sindico_id
                WHERE c.id_condominio = ?
                  AND s.usuario_id = ?
                  AND c.ativo = TRUE
            )
            """;

        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                condominioId,
                usuarioId
            )
        );
    }

    @Override
    public boolean condominioExiste(Integer condominioId) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_condominios
                WHERE id_condominio = ?
                  AND ativo = TRUE
            )
            """;

        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                condominioId
            )
        );
    }
}
