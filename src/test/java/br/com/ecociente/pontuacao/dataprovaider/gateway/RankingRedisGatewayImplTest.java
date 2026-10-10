package br.com.ecociente.pontuacao.dataprovaider.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.dataprovaider.gateway.RankingRedisGatewayImpl;

@ExtendWith(MockitoExtension.class)
class RankingRedisGatewayImplTest {

    private static final String KEY = "ecociente:ranking:moradores:7:2026-10-05_7d";

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RankingRedisGatewayImpl gateway;

    private OffsetDateTime expiresAt;
    private String expirationArgument;

    @BeforeEach
    void setUp() {
        expiresAt = OffsetDateTime.parse(
                "2027-01-10T00:00:00-03:00");

        expirationArgument = Long.toString(expiresAt.toEpochSecond());
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, 125L })
    @DisplayName("Deve enviar participante, saldo absoluto e expiração ao Redis")
    void shouldSendAbsoluteScoreAndExpirationToRedis(long points) {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(KEY)),
                eq("42"),
                eq(Long.toString(points)),
                eq(expirationArgument))).thenReturn(1L);

        assertDoesNotThrow(() -> gateway.definirPontuacao(KEY, 42, points, expiresAt));

        verify(redisTemplate).execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(KEY)),
                eq("42"),
                eq(Long.toString(points)),
                eq(expirationArgument));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = { -1L, 9_007_199_254_740_992L })
    @DisplayName("Deve rejeitar score inválido antes de acessar o Redis")
    void shouldRejectInvalidScoreBeforeCallingRedis(Long points) {
        InconsistenciaPontuacaoException ex = assertThrows(
                InconsistenciaPontuacaoException.class,
                () -> gateway.definirPontuacao(
                        KEY,
                        42,
                        points,
                        expiresAt));

        assertEquals("SCORE_INVALIDO", ex.getCodigoErro());
        verifyNoInteractions(redisTemplate);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = { 0L })
    @DisplayName("Deve falhar quando o Redis não confirmar a operação")
    void shouldFailWhenRedisDoesNotConfirmUpdate(Long confirmation) {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(KEY)),
                eq("42"),
                eq("125"),
                eq(expirationArgument))).thenReturn(confirmation);

        assertThrows(
                IllegalStateException.class,
                () -> gateway.definirPontuacao(
                        KEY,
                        42,
                        125L,
                        expiresAt));
    }

    @Test
    @DisplayName("Deve propagar falha de comunicação para permitir retry")
    void shouldPropagateRedisFailure() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(KEY)),
                eq("42"),
                eq("125"),
                eq(expirationArgument))).thenThrow(new IllegalStateException("Redis indisponível"));

        assertThrows(
                IllegalStateException.class,
                () -> gateway.definirPontuacao(
                        KEY,
                        42,
                        125L,
                        expiresAt));
    }

    @Test
    void deveConsultarPosicaoNoRedis() {
        var zSet = mock(
                org.springframework.data.redis.core.ZSetOperations.class);

        when(redisTemplate.opsForZSet()).thenReturn(zSet);

        when(zSet.reverseRank("ranking:teste", "42"))
                .thenReturn(2L);

        assertEquals(3L, gateway.buscarPosicao(
                "ranking:teste", 42));
    }

    @Test
    void deveRetornarNuloQuandoNaoExistePosicao() {
        var zSet = mock(
                org.springframework.data.redis.core.ZSetOperations.class);

        when(redisTemplate.opsForZSet()).thenReturn(zSet);

        when(zSet.reverseRank("ranking:teste", "999"))
                .thenReturn(null);

        assertNull(gateway.buscarPosicao("ranking:teste", 999));

        verify(zSet).reverseRank("ranking:teste", "999");
    }

    @Test
    void deveConsultarPontuacao() {
        var zSet = mock(
                org.springframework.data.redis.core.ZSetOperations.class);

        when(redisTemplate.opsForZSet()).thenReturn(zSet);

        when(zSet.score("ranking:teste", "42"))
                .thenReturn(125.0);

        assertEquals(125L, gateway.buscarPontuacao(
                "ranking:teste", 42));
    }

    @Test
    void deveContarParticipantes() {
        var zSet = mock(
                org.springframework.data.redis.core.ZSetOperations.class);

        when(redisTemplate.opsForZSet()).thenReturn(zSet);

        when(zSet.zCard("ranking:teste"))
                .thenReturn(15L);

        assertEquals(15L,
                gateway.contarParticipantes("ranking:teste"));
    }
}