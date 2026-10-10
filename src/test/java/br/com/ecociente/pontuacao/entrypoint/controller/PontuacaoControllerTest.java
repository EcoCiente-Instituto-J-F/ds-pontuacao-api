package br.com.ecociente.pontuacao.entrypoint.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.ecociente.pontuacao.core.domain.AcaoReconciliacaoType;
import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.OrigemPontuacaoType;
import br.com.ecociente.pontuacao.core.domain.Pagina;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;
import br.com.ecociente.pontuacao.core.exception.BusinessException;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.core.service.ConsultaPontuacaoService;
import br.com.ecociente.pontuacao.core.service.PontuacaoService;
import br.com.ecociente.pontuacao.entrypoint.dto.response.MovimentacaoPontosResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.PaginaResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.PontuacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.ReconciliacaoPontuacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.RedisSincronizacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.SaldoPontuacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.exception.GlobalExceptionHandler;
import br.com.ecociente.pontuacao.entrypoint.mapper.PontuacaoResponseMapper;

@ExtendWith(MockitoExtension.class)
class PontuacaoControllerTest {

  private static final String BASE_URL = "/api/v1/pontuacoes";

  private static final String POST_KEY = "postagem-10-credito";
  private static final String QUIZ_KEY = "tentativa-30-credito";
  private static final String RECONCILIATION_KEY = "postagem-10-ajuste-1";

  @Mock
  private PontuacaoService pontuacaoService;

  @Mock
  private ConsultaPontuacaoService consultaService;

  @Mock
  private PontuacaoResponseMapper mapper;

  @InjectMocks
  private PontuacaoController controller;

  private MockMvc mockMvc;
  private MovimentacaoPontos movimentacao;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    movimentacao = MovimentacaoPontos.builder()
        .id(100L)
        .usuarioId(42)
        .condominioId(7)
        .torreId(2)
        .categoriaId(3)
        .origemTipo(OrigemPontuacaoType.POSTAGEM)
        .postagemId(10)
        .tipoMovimentacao(TipoMovimentacaoType.CREDITO)
        .pontos(5)
        .redisSincronizado(false)
        .build();
  }

  @Nested
  @DisplayName("POST /postagens/{postagemId}/consolidar")
  class ConsolidatePost {

    @Test
    @DisplayName("Deve retornar os pontos concedidos à postagem")
    void shouldReturnPostCredit() throws Exception {
      var response = new PontuacaoResponse(
          OrigemPontuacaoType.POSTAGEM,
          10,
          null,
          42,
          3,
          5,
          TipoMovimentacaoType.CREDITO,
          100L,
          null,
          new RedisSincronizacaoResponse(false, "PENDENTE_RETRY"));

      when(pontuacaoService.consolidarPostagem(10, POST_KEY))
          .thenReturn(movimentacao);

      when(mapper.toPostagemResponse(10, movimentacao))
          .thenReturn(response);

      mockMvc.perform(post(BASE_URL + "/postagens/10/consolidar")
          .header("Idempotency-Key", POST_KEY))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.postagemId").value(10))
          .andExpect(jsonPath("$.usuarioId").value(42))
          .andExpect(jsonPath("$.pontosConcedidos").value(5))
          .andExpect(jsonPath("$.movimentacaoId").value(100))
          .andExpect(jsonPath("$.redis.sincronizado").value(false))
          .andExpect(jsonPath("$.redis.status").value("PENDENTE_RETRY"));

      verify(pontuacaoService).consolidarPostagem(10, POST_KEY);
      verify(mapper).toPostagemResponse(10, movimentacao);
    }

    @Test
    @DisplayName("Deve retornar zero pontos quando o teto estiver esgotado")
    void shouldReturnZeroPointsWhenDailyLimitIsReached() throws Exception {
      var response = new PontuacaoResponse(
          OrigemPontuacaoType.POSTAGEM,
          10,
          null,
          null,
          null,
          0,
          null,
          null,
          "TETO_DIARIO_ATINGIDO",
          null);

      when(pontuacaoService.consolidarPostagem(10, POST_KEY))
          .thenReturn(null);

      when(mapper.toPostagemResponse(10, null))
          .thenReturn(response);

      mockMvc.perform(post(BASE_URL + "/postagens/10/consolidar")
          .header("Idempotency-Key", POST_KEY))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.pontosConcedidos").value(0))
          .andExpect(jsonPath("$.motivo").value("TETO_DIARIO_ATINGIDO"))
          .andExpect(jsonPath("$.movimentacaoId").doesNotExist());
    }

    @Test
    @DisplayName("Deve rejeitar requisição sem chave de idempotência")
    void shouldRejectRequestWithoutIdempotencyKey() throws Exception {
      mockMvc.perform(post(BASE_URL + "/postagens/10/consolidar"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(pontuacaoService, mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "abc" })
    @DisplayName("Deve rejeitar identificador inválido da postagem")
    void shouldRejectInvalidPostId(String postId) throws Exception {
      mockMvc.perform(post(BASE_URL + "/postagens/{id}/consolidar", postId)
          .header("Idempotency-Key", POST_KEY))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(pontuacaoService, mapper);
    }

    @Test
    @DisplayName("Deve rejeitar chave de idempotência maior que o limite")
    void shouldRejectIdempotencyKeyExceedingMaximumLength() throws Exception {
      mockMvc.perform(post(BASE_URL + "/postagens/10/consolidar")
          .header("Idempotency-Key", "a".repeat(151)))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(pontuacaoService, mapper);
    }

    @Test
    @DisplayName("Deve rejeitar pontos enviados no corpo da requisição")
    void shouldRejectClientProvidedPoints() throws Exception {
      mockMvc.perform(post(BASE_URL + "/postagens/10/consolidar")
          .header("Idempotency-Key", POST_KEY)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "pontos": 9999
              }
              """))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(pontuacaoService, mapper);
    }

    @Test
    @DisplayName("Deve retornar 404 quando a postagem não existir")
    void shouldReturnNotFoundWhenPostDoesNotExist() throws Exception {
      when(pontuacaoService.consolidarPostagem(10, POST_KEY))
          .thenThrow(new NotFoundException(
              "POSTAGEM_NAO_ENCONTRADA",
              "Postagem não encontrada"));

      mockMvc.perform(post(BASE_URL + "/postagens/10/consolidar")
          .header("Idempotency-Key", POST_KEY))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.codigoError")
              .value("POSTAGEM_NAO_ENCONTRADA"));

      verifyNoInteractions(mapper);
    }
  }

  @Nested
  @DisplayName("POST /quizzes/tentativas/{tentativaId}/consolidar")
  class ConsolidateQuiz {

    @Test
    @DisplayName("Deve retornar a recompensa da tentativa")
    void shouldReturnQuizReward() throws Exception {
      movimentacao.setOrigemTipo(OrigemPontuacaoType.QUIZ);
      movimentacao.setPostagemId(null);
      movimentacao.setCategoriaId(null);
      movimentacao.setTentativaQuizId(30);
      movimentacao.setPontos(20);

      var response = new PontuacaoResponse(
          OrigemPontuacaoType.QUIZ,
          null,
          30,
          42,
          null,
          20,
          TipoMovimentacaoType.CREDITO,
          100L,
          null,
          new RedisSincronizacaoResponse(false, "PENDENTE_RETRY"));

      when(pontuacaoService.consolidarQuiz(30, QUIZ_KEY))
          .thenReturn(movimentacao);

      when(mapper.toPontuacaoResponse(movimentacao))
          .thenReturn(response);

      mockMvc.perform(post(BASE_URL + "/quizzes/tentativas/30/consolidar")
          .header("Idempotency-Key", QUIZ_KEY))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.tentativaId").value(30))
          .andExpect(jsonPath("$.pontosConcedidos").value(20))
          .andExpect(jsonPath("$.origemTipo").value("QUIZ"));

      verify(pontuacaoService).consolidarQuiz(30, QUIZ_KEY);
      verify(mapper).toPontuacaoResponse(movimentacao);
    }

    @Test
    @DisplayName("Deve retornar 409 quando a tentativa não foi aprovada")
    void shouldReturnConflictWhenAttemptIsNotApproved() throws Exception {
      when(pontuacaoService.consolidarQuiz(30, QUIZ_KEY))
          .thenThrow(new BusinessException(
              "QUIZ_NAO_APROVADO",
              "A tentativa não foi aprovada"));

      mockMvc.perform(post(BASE_URL + "/quizzes/tentativas/30/consolidar")
          .header("Idempotency-Key", QUIZ_KEY))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.codigoError").value("QUIZ_NAO_APROVADO"));

      verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("Deve rejeitar corpo na consolidação do quiz")
    void shouldRejectQuizRequestBody() throws Exception {
      mockMvc.perform(post(BASE_URL + "/quizzes/tentativas/30/consolidar")
          .header("Idempotency-Key", QUIZ_KEY)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(pontuacaoService, mapper);
    }
  }

  @Nested
  @DisplayName("POST /postagens/{postagemId}/reconciliar")
  class ReconcilePost {

    @Test
    @DisplayName("Deve retornar o estorno realizado")
    void shouldReturnReversalMovement() throws Exception {
      movimentacao.setTipoMovimentacao(TipoMovimentacaoType.ESTORNO);

      var response = new ReconciliacaoPontuacaoResponse(
          10,
          AcaoReconciliacaoType.ESTORNADO,
          5,
          100L,
          new RedisSincronizacaoResponse(false, "PENDENTE_RETRY"));

      when(pontuacaoService.reconciliarPostagem(10, RECONCILIATION_KEY))
          .thenReturn(movimentacao);

      when(mapper.toReconciliacaoResponse(10, movimentacao))
          .thenReturn(response);

      mockMvc.perform(post(BASE_URL + "/postagens/10/reconciliar")
          .header("Idempotency-Key", RECONCILIATION_KEY))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.acao").value("ESTORNADO"))
          .andExpect(jsonPath("$.pontos").value(5));

      verify(pontuacaoService)
          .reconciliarPostagem(10, RECONCILIATION_KEY);
    }

    @Test
    @DisplayName("Deve retornar ausência de alteração")
    void shouldReturnNoChangeResponse() throws Exception {
      var response = new ReconciliacaoPontuacaoResponse(
          10,
          AcaoReconciliacaoType.SEM_ALTERACAO,
          0,
          null,
          null);

      when(pontuacaoService.reconciliarPostagem(10, RECONCILIATION_KEY))
          .thenReturn(null);

      when(mapper.toReconciliacaoResponse(10, null))
          .thenReturn(response);

      mockMvc.perform(post(BASE_URL + "/postagens/10/reconciliar")
          .header("Idempotency-Key", RECONCILIATION_KEY))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.acao").value("SEM_ALTERACAO"))
          .andExpect(jsonPath("$.pontos").value(0));
    }

    @Test
    @DisplayName("Deve rejeitar corpo na reconciliação")
    void shouldRejectReconciliationRequestBody() throws Exception {
      mockMvc.perform(post(BASE_URL + "/postagens/10/reconciliar")
          .header("Idempotency-Key", RECONCILIATION_KEY)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(pontuacaoService, mapper);
    }
  }

  @Nested
  @DisplayName("GET /usuarios/{usuarioId}/saldo")
  class GetBalance {

    @Test
    @DisplayName("Deve retornar o saldo do usuário")
    void shouldReturnUserBalance() throws Exception {
      when(consultaService.consultarSaldo(42)).thenReturn(125L);

      when(mapper.toSaldoResponse(42, 125L))
          .thenReturn(new SaldoPontuacaoResponse(42, 125L));

      mockMvc.perform(get(BASE_URL + "/usuarios/42/saldo"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.usuarioId").value(42))
          .andExpect(jsonPath("$.saldo").value(125));

      verify(consultaService).consultarSaldo(42);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "abc" })
    @DisplayName("Deve rejeitar identificador inválido do usuário")
    void shouldRejectInvalidUserId(String userId) throws Exception {
      mockMvc.perform(get(BASE_URL + "/usuarios/{id}/saldo", userId))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(consultaService, mapper);
    }

    @Test
    @DisplayName("Deve retornar 500 sem expor erro interno do service")
    void shouldHideUnexpectedServiceError() throws Exception {
      when(consultaService.consultarSaldo(42))
          .thenThrow(new RuntimeException("Detalhe sensível"));

      mockMvc.perform(get(BASE_URL + "/usuarios/42/saldo"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.codigoError").value("ERRO_INTERNO"))
          .andExpect(jsonPath("$.details[0].message")
              .value("Erro interno do servidor"));

      verifyNoInteractions(mapper);
    }
  }

  @Nested
  @DisplayName("GET /usuarios/{usuarioId}/movimentacoes")
  class GetMovementHistory {

    @Test
    @DisplayName("Deve utilizar a paginação padrão quando não informada")
    void shouldUseDefaultPagination() throws Exception {
      Pagina<MovimentacaoPontos> pagina = new Pagina<>(
          List.of(),
          0,
          20,
          0L,
          0);

      PaginaResponse<MovimentacaoPontosResponse> response = new PaginaResponse<>(
          List.of(),
          0,
          20,
          0L,
          0);

      when(consultaService.listarMovimentacoes(42, 0, 20))
          .thenReturn(pagina);

      when(mapper.toPaginaResponse(pagina)).thenReturn(response);

      mockMvc.perform(get(BASE_URL + "/usuarios/42/movimentacoes"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.pagina").value(0))
          .andExpect(jsonPath("$.tamanho").value(20))
          .andExpect(jsonPath("$.conteudo").isEmpty());

      verify(consultaService).listarMovimentacoes(42, 0, 20);
    }

    @Test
    @DisplayName("Deve encaminhar a paginação informada para o service")
    void shouldForwardRequestedPagination() throws Exception {
      Pagina<MovimentacaoPontos> pagina = new Pagina<>(
          List.of(),
          2,
          5,
          0L,
          0);

      PaginaResponse<MovimentacaoPontosResponse> response = new PaginaResponse<>(
          List.of(),
          2,
          5,
          0L,
          0);

      when(consultaService.listarMovimentacoes(42, 2, 5))
          .thenReturn(pagina);

      when(mapper.toPaginaResponse(pagina)).thenReturn(response);

      mockMvc.perform(get(BASE_URL + "/usuarios/42/movimentacoes")
          .param("pagina", "2")
          .param("tamanho", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.pagina").value(2))
          .andExpect(jsonPath("$.tamanho").value(5));

      verify(consultaService).listarMovimentacoes(42, 2, 5);
    }

    @Test
    @DisplayName("Deve rejeitar página negativa")
    void shouldRejectNegativePage() throws Exception {
      mockMvc.perform(get(BASE_URL + "/usuarios/42/movimentacoes")
          .param("pagina", "-1"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(consultaService, mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "101" })
    @DisplayName("Deve rejeitar tamanho de página fora do limite")
    void shouldRejectInvalidPageSize(String size) throws Exception {
      mockMvc.perform(get(BASE_URL + "/usuarios/42/movimentacoes")
          .param("tamanho", size))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(consultaService, mapper);
    }
  }
}