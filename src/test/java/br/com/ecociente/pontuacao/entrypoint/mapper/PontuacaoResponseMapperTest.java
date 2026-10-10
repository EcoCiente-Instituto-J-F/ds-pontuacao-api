package br.com.ecociente.pontuacao.entrypoint.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.ecociente.pontuacao.core.domain.AcaoReconciliacaoType;
import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.OrigemPontuacaoType;
import br.com.ecociente.pontuacao.core.domain.Pagina;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;

class PontuacaoResponseMapperTest {

  private PontuacaoResponseMapper mapper;
  private MovimentacaoPontos movimentacao;

  @BeforeEach
  void setUp() {
    mapper = new PontuacaoResponseMapper();

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

  @Test
  @DisplayName("Deve representar teto esgotado sem movimentação")
  void shouldMapMissingPostCreditToDailyLimitResponse() {
    var response = mapper.toPostagemResponse(10, null);

    assertEquals(OrigemPontuacaoType.POSTAGEM, response.origemTipo());
    assertEquals(10, response.postagemId());
    assertEquals(0, response.pontosConcedidos());
    assertEquals("TETO_DIARIO_ATINGIDO", response.motivo());
    assertNull(response.movimentacaoId());
    assertNull(response.redis());
  }

  @Test
  @DisplayName("Deve mapear crédito da postagem com sincronização pendente")
  void shouldMapPostCreditWithPendingSynchronization() {
    var response = mapper.toPostagemResponse(10, movimentacao);

    assertEquals(100L, response.movimentacaoId());
    assertEquals(42, response.usuarioId());
    assertEquals(3, response.categoriaId());
    assertEquals(5, response.pontosConcedidos());
    assertNull(response.motivo());
    assertNotNull(response.redis());
    assertFalse(response.redis().sincronizado());
    assertEquals("PENDENTE_RETRY", response.redis().status());
  }

  @Test
  @DisplayName("Deve indicar sincronização concluída")
  void shouldMapSynchronizedMovement() {
    movimentacao.setRedisSincronizado(true);

    var response = mapper.toPontuacaoResponse(movimentacao);

    assertTrue(response.redis().sincronizado());
    assertEquals("SINCRONIZADO", response.redis().status());
  }

  @Test
  @DisplayName("Deve mapear crédito de quiz com identificador da tentativa")
  void shouldMapQuizCredit() {
    movimentacao.setOrigemTipo(OrigemPontuacaoType.QUIZ);
    movimentacao.setTentativaQuizId(30);
    movimentacao.setPostagemId(null);
    movimentacao.setCategoriaId(null);

    var response = mapper.toPontuacaoResponse(movimentacao);

    assertEquals(OrigemPontuacaoType.QUIZ, response.origemTipo());
    assertEquals(30, response.tentativaId());
    assertNull(response.postagemId());
    assertNull(response.categoriaId());
  }

  @Test
  @DisplayName("Deve representar reconciliação sem alteração")
  void shouldMapReconciliationWithoutChanges() {
    var response = mapper.toReconciliacaoResponse(10, null);

    assertEquals(10, response.postagemId());
    assertEquals(AcaoReconciliacaoType.SEM_ALTERACAO, response.acao());
    assertEquals(0, response.pontos());
    assertNull(response.movimentacaoId());
    assertNull(response.redis());
  }

  @ParameterizedTest
  @CsvSource({
      "ESTORNO, ESTORNADO",
      "RESTAURACAO, RESTAURADO"
  })
  @DisplayName("Deve mapear o tipo de ajuste para a ação correspondente")
  void shouldMapAdjustmentToReconciliationAction(
      TipoMovimentacaoType movementType,
      AcaoReconciliacaoType expectedAction) {
    movimentacao.setTipoMovimentacao(movementType);

    var response = mapper.toReconciliacaoResponse(10, movimentacao);

    assertEquals(expectedAction, response.acao());
    assertEquals(5, response.pontos());
    assertEquals(100L, response.movimentacaoId());
  }

  @Test
  @DisplayName("Deve preservar os dados de paginação do histórico")
  void shouldPreserveHistoryPagination() {
    var pagina = new Pagina<>(
        List.of(movimentacao),
        1,
        20,
        21L,
        2);

    var response = mapper.toPaginaResponse(pagina);

    assertEquals(1, response.pagina());
    assertEquals(20, response.tamanho());
    assertEquals(21L, response.totalElementos());
    assertEquals(2, response.totalPaginas());
    assertEquals(1, response.conteudo().size());
    assertEquals(
        100L,
        response.conteudo().get(0).movimentacaoId());
  }
}