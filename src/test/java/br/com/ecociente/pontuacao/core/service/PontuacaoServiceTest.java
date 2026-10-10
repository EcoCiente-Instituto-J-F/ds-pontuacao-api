package br.com.ecociente.pontuacao.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.OrigemPontuacaoType;
import br.com.ecociente.pontuacao.core.domain.Postagem;
import br.com.ecociente.pontuacao.core.domain.Quiz;
import br.com.ecociente.pontuacao.core.domain.TentativaQuiz;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;
import br.com.ecociente.pontuacao.core.exception.AlreadyExistsException;
import br.com.ecociente.pontuacao.core.exception.BusinessException;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.core.gateway.CategoriaResiduoGateway;
import br.com.ecociente.pontuacao.core.gateway.MovimentacaoPontosGateway;
import br.com.ecociente.pontuacao.core.gateway.PostagemGateway;
import br.com.ecociente.pontuacao.core.gateway.QuizGateway;
import br.com.ecociente.pontuacao.core.gateway.TentativaQuizGateway;

@ExtendWith(MockitoExtension.class)
class PontuacaoServiceTest {

  private static final String CREDIT_KEY = "postagem-10-credito";
  private static final String QUIZ_KEY = "tentativa-30-credito";
  private static final String RECONCILIATION_KEY = "postagem-10-ajuste-1";

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

  private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

  @Mock
  private PostagemGateway postagemGateway;

  @Mock
  private CategoriaResiduoGateway categoriaGateway;

  @Mock
  private QuizGateway quizGateway;

  @Mock
  private TentativaQuizGateway tentativaGateway;

  @Mock
  private MovimentacaoPontosGateway movimentacaoGateway;

  @Mock
  private Clock clock;

  @InjectMocks
  private PontuacaoService service;

  private Postagem postagem;
  private TentativaQuiz tentativa;
  private Quiz quiz;
  private MovimentacaoPontos credito;

  @BeforeEach
  void setUp() {
    postagem = Postagem.builder()
        .id(10)
        .usuarioId(42)
        .condominioId(7)
        .torreId(2)
        .categoriaId(3)
        .saldoConfianca(0)
        .pontuacaoAtiva(true)
        .pontuacaoReconciliacaoPendente(true)
        .dataPostagem(
            OffsetDateTime.parse("2026-10-10T08:00:00-03:00"))
        .dataLimiteAnalise(
            OffsetDateTime.parse("2026-10-11T08:00:00-03:00"))
        .build();

    tentativa = TentativaQuiz.builder()
        .id(30)
        .usuarioId(42)
        .quizId(12)
        .condominioId(7)
        .torreId(2)
        .aprovado(true)
        .concluidoEm(
            OffsetDateTime.parse("2026-10-10T08:30:00-03:00"))
        .build();

    quiz = Quiz.builder()
        .id(12)
        .tituloQuiz("Reciclagem")
        .pontosRecompensa(20)
        .ativo(true)
        .build();

    credito = MovimentacaoPontos.builder()
        .id(100L)
        .usuarioId(42)
        .condominioId(7)
        .torreId(2)
        .categoriaId(3)
        .origemTipo(OrigemPontuacaoType.POSTAGEM)
        .postagemId(10)
        .tipoMovimentacao(TipoMovimentacaoType.CREDITO)
        .pontos(5)
        .idempotencyKey(CREDIT_KEY)
        .redisSincronizado(false)
        .tentativasSyncRedis(0)
        .build();
  }

  @Nested
  @DisplayName("consolidarPostagem")
  class ConsolidatePost {

    @ParameterizedTest
    @ValueSource(ints = { 10, 5 })
    @DisplayName("Deve conceder exatamente os pontos calculados pelo banco")
    void shouldCreatePostCreditUsingCalculatedPoints(int points) {
      preparePostLock();
      prepareClock();

      when(categoriaGateway.calcularPontosDisponiveis(
          42,
          3,
          postagem.getDataPostagem(),
          ZONE.getId())).thenReturn(points);

      prepareMovementCreation();

      MovimentacaoPontos resultado = service.consolidarPostagem(10, CREDIT_KEY);

      assertNotNull(resultado);
      assertEquals(200L, resultado.getId());

      var captor = ArgumentCaptor.forClass(MovimentacaoPontos.class);

      verify(movimentacaoGateway).criar(captor.capture());

      MovimentacaoPontos enviada = captor.getValue();

      assertEquals(points, enviada.getPontos());
      assertEquals(42, enviada.getUsuarioId());
      assertEquals(7, enviada.getCondominioId());
      assertEquals(2, enviada.getTorreId());
      assertEquals(3, enviada.getCategoriaId());
      assertEquals(10, enviada.getPostagemId());
      assertNull(enviada.getTentativaQuizId());
      assertNull(enviada.getMovimentacaoReferenciaId());
      assertEquals(
          OrigemPontuacaoType.POSTAGEM,
          enviada.getOrigemTipo());
      assertEquals(
          TipoMovimentacaoType.CREDITO,
          enviada.getTipoMovimentacao());
      assertEquals(CREDIT_KEY, enviada.getIdempotencyKey());
      assertEquals(
          OffsetDateTime.ofInstant(NOW, ZONE),
          enviada.getOcorridoEm());
      assertEquals(Boolean.FALSE, enviada.getRedisSincronizado());
      assertEquals(0, enviada.getTentativasSyncRedis());

      var ordem = inOrder(
          postagemGateway,
          categoriaGateway,
          movimentacaoGateway);

      ordem.verify(postagemGateway).buscarUsuarioId(10);
      ordem.verify(postagemGateway).bloquearUsuario(42);
      ordem.verify(postagemGateway).buscarComBloqueio(10);

      ordem.verify(categoriaGateway).calcularPontosDisponiveis(
          42,
          3,
          postagem.getDataPostagem(),
          ZONE.getId());

      ordem.verify(movimentacaoGateway)
          .criar(any(MovimentacaoPontos.class));

      ordem.verify(postagemGateway)
          .marcarReconciliacaoConcluida(10);
    }

    @Test
    @DisplayName("Deve retornar null quando o teto diário estiver esgotado")
    void shouldReturnNullWhenDailyLimitIsReached() {
      preparePostLock();

      when(clock.getZone()).thenReturn(ZONE);

      when(categoriaGateway.calcularPontosDisponiveis(
          42,
          3,
          postagem.getDataPostagem(),
          ZONE.getId())).thenReturn(0);

      MovimentacaoPontos resultado = service.consolidarPostagem(10, CREDIT_KEY);

      assertNull(resultado);
      verify(movimentacaoGateway, never()).criar(any());
    }

    @Test
    @DisplayName("Deve retornar o crédito existente sem conceder novamente")
    void shouldReturnExistingCreditWithoutCreatingAnother() {
      preparePostLock();

      when(movimentacaoGateway.buscarCreditoPostagem(10))
          .thenReturn(Optional.of(credito));

      MovimentacaoPontos resultado = service.consolidarPostagem(10, CREDIT_KEY);

      assertSame(credito, resultado);
      verify(movimentacaoGateway, never()).criar(any());
      verifyNoInteractions(categoriaGateway);
    }

    @Test
    @DisplayName("Deve rejeitar postagem com pontuação suspensa")
    void shouldRejectPostWithInactiveScoring() {
      postagem.setPontuacaoAtiva(false);
      preparePostLock();

      BusinessException ex = assertThrows(
          BusinessException.class,
          () -> service.consolidarPostagem(10, CREDIT_KEY));

      assertEquals(
          "POSTAGEM_SEM_PONTUACAO_ATIVA",
          ex.getCodigoErro());

      verify(movimentacaoGateway, never()).criar(any());
      verifyNoInteractions(categoriaGateway);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a postagem não existir")
    void shouldThrowWhenPostDoesNotExist() {
      when(postagemGateway.buscarUsuarioId(10))
          .thenReturn(Optional.empty());

      NotFoundException ex = assertThrows(
          NotFoundException.class,
          () -> service.consolidarPostagem(10, CREDIT_KEY));

      assertEquals("POSTAGEM_NAO_ENCONTRADA", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
    }

    @Test
    @DisplayName("Deve rejeitar mudança de usuário após adquirir o bloqueio")
    void shouldRejectPostOwnerChangeAfterLock() {
      when(postagemGateway.buscarUsuarioId(10)).thenReturn(
          Optional.of(42));
      when(postagemGateway.bloquearUsuario(42)).thenReturn(true);

      postagem.setUsuarioId(99);

      when(postagemGateway.buscarComBloqueio(10))
          .thenReturn(Optional.of(postagem));

      BusinessException ex = assertThrows(
          BusinessException.class,
          () -> service.consolidarPostagem(10, CREDIT_KEY));

      assertEquals("POSTAGEM_ALTERADA", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = { -1 })
    @DisplayName("Deve rejeitar valor inválido retornado pelo cálculo")
    void shouldRejectInvalidCalculatedPoints(Integer points) {
      preparePostLock();

      when(clock.getZone()).thenReturn(ZONE);

      when(categoriaGateway.calcularPontosDisponiveis(
          42,
          3,
          postagem.getDataPostagem(),
          ZONE.getId())).thenReturn(points);

      InconsistenciaPontuacaoException ex = assertThrows(
          InconsistenciaPontuacaoException.class,
          () -> service.consolidarPostagem(10, CREDIT_KEY));

      assertEquals("PONTUACAO_INVALIDA", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
    }

    @Test
    @DisplayName("Deve rejeitar chave já usada em outra postagem")
    void shouldRejectIdempotencyKeyUsedByAnotherPost() {
      preparePostLock();

      credito.setPostagemId(99);

      when(movimentacaoGateway.buscarPorIdempotencyKey(CREDIT_KEY))
          .thenReturn(Optional.of(credito));

      AlreadyExistsException ex = assertThrows(
          AlreadyExistsException.class,
          () -> service.consolidarPostagem(10, CREDIT_KEY));

      assertEquals("IDEMPOTENCY_KEY_EM_USO", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
    }
  }

  @Nested
  @DisplayName("consolidarQuiz")
  class ConsolidateQuiz {

    @Test
    @DisplayName("Deve conceder a recompensa configurada no quiz")
    void shouldCreateQuizCreditUsingConfiguredReward() {
      prepareEligibleQuiz();
      prepareClock();
      prepareMovementCreation();

      MovimentacaoPontos resultado = service.consolidarQuiz(30, QUIZ_KEY);

      assertNotNull(resultado);
      assertEquals(20, resultado.getPontos());
      assertEquals(30, resultado.getTentativaQuizId());
      assertEquals(42, resultado.getUsuarioId());
      assertEquals(7, resultado.getCondominioId());
      assertEquals(2, resultado.getTorreId());
      assertEquals(OrigemPontuacaoType.QUIZ, resultado.getOrigemTipo());
      assertEquals(
          TipoMovimentacaoType.CREDITO,
          resultado.getTipoMovimentacao());
      assertNull(resultado.getPostagemId());
      assertNull(resultado.getCategoriaId());
      assertEquals(Boolean.FALSE, resultado.getRedisSincronizado());

      verifyNoInteractions(categoriaGateway);
    }

    @Test
    @DisplayName("Deve permitir crédito pessoal sem condomínio e torre")
    void shouldCreatePersonalCreditWithoutCondominium() {
      tentativa.setCondominioId(null);
      tentativa.setTorreId(null);

      prepareEligibleQuiz();
      prepareClock();
      prepareMovementCreation();

      MovimentacaoPontos resultado = service.consolidarQuiz(30, QUIZ_KEY);

      assertEquals(20, resultado.getPontos());
      assertNull(resultado.getCondominioId());
      assertNull(resultado.getTorreId());
    }

    @Test
    @DisplayName("Deve rejeitar tentativa não concluída")
    void shouldRejectUnfinishedAttempt() {
      tentativa.setConcluidoEm(null);
      prepareAttemptLock();

      BusinessException ex = assertThrows(
          BusinessException.class,
          () -> service.consolidarQuiz(30, QUIZ_KEY));

      assertEquals("TENTATIVA_NAO_CONCLUIDA", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
      verifyNoInteractions(quizGateway);
    }

    @Test
    @DisplayName("Deve rejeitar tentativa reprovada")
    void shouldRejectFailedAttempt() {
      tentativa.setAprovado(false);
      prepareAttemptLock();

      BusinessException ex = assertThrows(
          BusinessException.class,
          () -> service.consolidarQuiz(30, QUIZ_KEY));

      assertEquals("QUIZ_NAO_APROVADO", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
    }

    @Test
    @DisplayName("Deve rejeitar quiz inativo")
    void shouldRejectInactiveQuiz() {
      quiz.setAtivo(false);
      prepareEligibleQuiz();

      BusinessException ex = assertThrows(
          BusinessException.class,
          () -> service.consolidarQuiz(30, QUIZ_KEY));

      assertEquals("QUIZ_INATIVO", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
    }

    @Test
    @DisplayName("Deve retornar crédito existente sem duplicar a recompensa")
    void shouldReturnExistingQuizCreditWithoutDuplicatingReward() {
      prepareAttemptLock();

      credito.setOrigemTipo(OrigemPontuacaoType.QUIZ);
      credito.setPostagemId(null);
      credito.setCategoriaId(null);
      credito.setTentativaQuizId(30);

      when(movimentacaoGateway.buscarCreditoTentativa(30))
          .thenReturn(Optional.of(credito));

      MovimentacaoPontos resultado = service.consolidarQuiz(30, QUIZ_KEY);

      assertSame(credito, resultado);
      verify(movimentacaoGateway, never()).criar(any());
      verifyNoInteractions(quizGateway);
    }

    @Test
    @DisplayName("Deve rejeitar torre sem condomínio")
    void shouldRejectTowerWithoutCondominium() {
      tentativa.setCondominioId(null);
      prepareEligibleQuiz();

      BusinessException ex = assertThrows(
          BusinessException.class,
          () -> service.consolidarQuiz(30, QUIZ_KEY));

      assertEquals(
          "CONTEXTO_CONDOMINIAL_INVALIDO",
          ex.getCodigoErro());

      verify(movimentacaoGateway, never()).criar(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Deve rejeitar recompensa inválida")
    void shouldRejectInvalidQuizReward(Integer reward) {
      quiz.setPontosRecompensa(reward);
      prepareEligibleQuiz();

      InconsistenciaPontuacaoException ex = assertThrows(
          InconsistenciaPontuacaoException.class,
          () -> service.consolidarQuiz(30, QUIZ_KEY));

      assertEquals("RECOMPENSA_INVALIDA", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
    }
  }

  @Nested
  @DisplayName("reconciliarPostagem")
  class ReconcilePost {

    @Test
    @DisplayName("Deve estornar exatamente o valor do crédito original")
    void shouldReverseOriginalCreditWhenScoringIsInactive() {
      postagem.setPontuacaoAtiva(false);
      postagem.setSaldoConfianca(-5);

      prepareReconciliation(5L);
      prepareClock();
      prepareMovementCreation();

      MovimentacaoPontos resultado = service.reconciliarPostagem(10, RECONCILIATION_KEY);

      assertEquals(
          TipoMovimentacaoType.ESTORNO,
          resultado.getTipoMovimentacao());
      assertEquals(5, resultado.getPontos());
      assertEquals(100L, resultado.getMovimentacaoReferenciaId());
      assertEquals(42, resultado.getUsuarioId());
      assertEquals(7, resultado.getCondominioId());
      assertEquals(2, resultado.getTorreId());
      assertEquals(3, resultado.getCategoriaId());

      verify(postagemGateway).marcarReconciliacaoConcluida(10);
    }

    @Test
    @DisplayName("Deve restaurar o crédito sem recalcular o teto")
    void shouldRestoreOriginalCreditWithoutRecalculatingLimit() {
      postagem.setPontuacaoAtiva(true);
      postagem.setSaldoConfianca(0);

      prepareReconciliation(0L);
      prepareClock();
      prepareMovementCreation();

      MovimentacaoPontos resultado = service.reconciliarPostagem(10, RECONCILIATION_KEY);

      assertEquals(
          TipoMovimentacaoType.RESTAURACAO,
          resultado.getTipoMovimentacao());
      assertEquals(5, resultado.getPontos());
      assertEquals(100L, resultado.getMovimentacaoReferenciaId());

      verifyNoInteractions(categoriaGateway);
    }

    @Test
    @DisplayName("Deve preservar o contexto do crédito original no ajuste")
    void shouldPreserveOriginalCreditContextDuringReconciliation() {
      postagem.setPontuacaoAtiva(false);
      postagem.setCondominioId(88);
      postagem.setTorreId(99);
      postagem.setCategoriaId(44);

      prepareReconciliation(5L);
      prepareClock();
      prepareMovementCreation();

      MovimentacaoPontos resultado = service.reconciliarPostagem(10, RECONCILIATION_KEY);

      assertEquals(7, resultado.getCondominioId());
      assertEquals(2, resultado.getTorreId());
      assertEquals(3, resultado.getCategoriaId());
    }

    @Test
    @DisplayName("Não deve ajustar postagem ativa que já possui os pontos")
    void shouldNotCreateMovementWhenActivePostAlreadyHasCredit() {
      prepareReconciliation(5L);

      MovimentacaoPontos resultado = service.reconciliarPostagem(10, RECONCILIATION_KEY);

      assertNull(resultado);
      verify(movimentacaoGateway, never()).criar(any());
      verify(postagemGateway).marcarReconciliacaoConcluida(10);
    }

    @Test
    @DisplayName("Não deve estornar novamente quando o saldo já é zero")
    void shouldNotReverseCreditAgainWhenBalanceIsZero() {
      postagem.setPontuacaoAtiva(false);
      prepareReconciliation(0L);

      MovimentacaoPontos resultado = service.reconciliarPostagem(10, RECONCILIATION_KEY);

      assertNull(resultado);
      verify(movimentacaoGateway, never()).criar(any());
    }

    @Test
    @DisplayName("Deve retornar ajuste existente ao repetir a mesma chave")
    void shouldReturnExistingAdjustmentForRepeatedKey() {
      preparePostLock();

      MovimentacaoPontos estorno = MovimentacaoPontos.builder()
          .id(101L)
          .origemTipo(OrigemPontuacaoType.POSTAGEM)
          .postagemId(10)
          .tipoMovimentacao(TipoMovimentacaoType.ESTORNO)
          .pontos(5)
          .movimentacaoReferenciaId(100L)
          .build();

      when(movimentacaoGateway.buscarPorIdempotencyKey(
          RECONCILIATION_KEY)).thenReturn(Optional.of(estorno));

      MovimentacaoPontos resultado = service.reconciliarPostagem(10, RECONCILIATION_KEY);

      assertSame(estorno, resultado);
      verify(movimentacaoGateway, never()).criar(any());
    }

    @ParameterizedTest
    @ValueSource(longs = { -5L, 3L, 10L })
    @DisplayName("Deve rejeitar saldo incompatível com o crédito original")
    void shouldRejectBalanceInconsistentWithOriginalCredit(long balance) {
      prepareReconciliation(balance);

      InconsistenciaPontuacaoException ex = assertThrows(
          InconsistenciaPontuacaoException.class,
          () -> service.reconciliarPostagem(
              10,
              RECONCILIATION_KEY));

      assertEquals("INCONSISTENCIA_MOVIMENTACAO", ex.getCodigoErro());
      verify(movimentacaoGateway, never()).criar(any());
    }

    @Test
    @DisplayName("Não deve criar ajuste se não existe crédito e o saldo é zero")
    void shouldReturnNullWhenNoOriginalCreditExists() {
      preparePostLock();

      when(movimentacaoGateway.calcularSaldoPostagem(10))
          .thenReturn(0L);

      when(movimentacaoGateway.buscarCreditoPostagem(10))
          .thenReturn(Optional.empty());

      MovimentacaoPontos resultado = service.reconciliarPostagem(10, RECONCILIATION_KEY);

      assertNull(resultado);
      verify(movimentacaoGateway, never()).criar(any());
    }
  }

  @Nested
  @DisplayName("Validação da chave de idempotência")
  class ValidateIdempotencyKey {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   " })
    @DisplayName("Deve rejeitar chave ausente ou em branco")
    void shouldRejectMissingOrBlankKey(String key) {
      BusinessException ex = assertThrows(
          BusinessException.class,
          () -> service.consolidarPostagem(10, key));

      assertEquals("IDEMPOTENCY_KEY_INVALIDA", ex.getCodigoErro());
      verifyNoInteractions(postagemGateway, movimentacaoGateway);
    }

    @Test
    @DisplayName("Deve rejeitar chave maior que 150 caracteres")
    void shouldRejectKeyLongerThanMaximumLength() {
      String key = "a".repeat(151);

      BusinessException ex = assertThrows(
          BusinessException.class,
          () -> service.consolidarQuiz(30, key));

      assertEquals("IDEMPOTENCY_KEY_INVALIDA", ex.getCodigoErro());
      verifyNoInteractions(tentativaGateway, movimentacaoGateway);
    }
  }

  private void preparePostLock() {
    when(postagemGateway.buscarUsuarioId(10))
        .thenReturn(Optional.of(42));

    when(postagemGateway.bloquearUsuario(42))
        .thenReturn(true);

    when(postagemGateway.buscarComBloqueio(10))
        .thenReturn(Optional.of(postagem));
  }

  private void prepareAttemptLock() {
    when(tentativaGateway.buscarComBloqueio(30))
        .thenReturn(Optional.of(tentativa));
  }

  private void prepareEligibleQuiz() {
    prepareAttemptLock();

    when(quizGateway.buscarPorId(12))
        .thenReturn(Optional.of(quiz));
  }

  private void prepareReconciliation(long balance) {
    preparePostLock();

    when(movimentacaoGateway.calcularSaldoPostagem(10))
        .thenReturn(balance);

    when(movimentacaoGateway.buscarCreditoPostagem(10))
        .thenReturn(Optional.of(credito));
  }

  private void prepareClock() {
    when(clock.getZone()).thenReturn(ZONE);
    when(clock.instant()).thenReturn(NOW);
  }

  private void prepareMovementCreation() {
    when(movimentacaoGateway.criar(any(MovimentacaoPontos.class)))
        .thenAnswer(invocation -> {
          MovimentacaoPontos movimentacao = invocation.getArgument(0);

          movimentacao.setId(200L);
          return movimentacao;
        });
  }
}