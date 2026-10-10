
package br.com.ecociente.pontuacao.dataprovaider.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class VinculoCondominialGatewayImplTest {

    private JdbcTemplate jdbcTemplate;
    private VinculoCondominialGatewayImpl gateway;

    @BeforeEach
    void configurar() {
        jdbcTemplate = mock(JdbcTemplate.class);
        gateway = new VinculoCondominialGatewayImpl(jdbcTemplate);
    }

    @Test
    void devePermitirVinculoAtivo() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Boolean.class), eq(42), eq(7))).thenReturn(true);

        assertTrue(gateway.possuiVinculoAtivo(42, 7));
    }

    @Test
    void deveNegarVinculoInativo() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Boolean.class), eq(42), eq(7))).thenReturn(false);

        assertFalse(gateway.possuiVinculoAtivo(42, 7));
    }

    @Test
    void deveValidarTorreDoCondominio() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Boolean.class), eq(5), eq(7))).thenReturn(true);

        assertTrue(gateway.torrePertenceCondominio(5, 7));
    }

    @Test
    void deveNegarTorreDeOutroCondominio() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Boolean.class), eq(5), eq(8))).thenReturn(false);

        assertFalse(gateway.torrePertenceCondominio(5, 8));
    }

    @Test
    void deveIdentificarSindicoResponsavel() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Boolean.class), eq(7), eq(42))).thenReturn(true);

        assertTrue(gateway.ehSindico(42, 7));
    }

    @Test
    void deveNegarSindicoDeOutroCondominio() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Boolean.class), eq(8), eq(42))).thenReturn(false);

        assertFalse(gateway.ehSindico(42, 8));
    }

    @Test
    void deveVerificarCondominioAtivo() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Boolean.class), eq(7))).thenReturn(true);

        assertTrue(gateway.condominioExiste(7));
    }

    @Test
    void deveRetornarFalseQuandoConsultaRetornarNulo() {
        assertFalse(gateway.condominioExiste(999));
    }
}
