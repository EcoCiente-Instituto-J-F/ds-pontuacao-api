package br.com.ecociente.pontuacao.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.pontuacao.core.domain.AcaoReconciliacaoType;
import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.OrigemPontuacaoType;
import br.com.ecociente.pontuacao.core.domain.Pagina;
import br.com.ecociente.pontuacao.core.domain.TipoMovimentacaoType;
import br.com.ecociente.pontuacao.entrypoint.dto.response.MovimentacaoPontosResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.PaginaResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.PontuacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.ReconciliacaoPontuacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.RedisSincronizacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.SaldoPontuacaoResponse;

@Component
public class PontuacaoResponseMapper {

  public PontuacaoResponse toPostagemResponse(
      Integer postagemId,
      MovimentacaoPontos movimentacao) {
    if (movimentacao == null) {
      return new PontuacaoResponse(
          OrigemPontuacaoType.POSTAGEM,
          postagemId,
          null,
          null,
          null,
          0,
          null,
          null,
          "TETO_DIARIO_ATINGIDO",
          null);
    }

    return toPontuacaoResponse(movimentacao);
  }

  public PontuacaoResponse toPontuacaoResponse(
      MovimentacaoPontos movimentacao) {
    return new PontuacaoResponse(
        movimentacao.getOrigemTipo(),
        movimentacao.getPostagemId(),
        movimentacao.getTentativaQuizId(),
        movimentacao.getUsuarioId(),
        movimentacao.getCategoriaId(),
        movimentacao.getPontos(),
        movimentacao.getTipoMovimentacao(),
        movimentacao.getId(),
        null,
        toRedisResponse(movimentacao));
  }

  public ReconciliacaoPontuacaoResponse toReconciliacaoResponse(
      Integer postagemId,
      MovimentacaoPontos movimentacao) {
    if (movimentacao == null) {
      return new ReconciliacaoPontuacaoResponse(
          postagemId,
          AcaoReconciliacaoType.SEM_ALTERACAO,
          0,
          null,
          null);
    }

    AcaoReconciliacaoType acao = movimentacao.getTipoMovimentacao() == TipoMovimentacaoType.ESTORNO
        ? AcaoReconciliacaoType.ESTORNADO
        : AcaoReconciliacaoType.RESTAURADO;

    return new ReconciliacaoPontuacaoResponse(
        postagemId,
        acao,
        movimentacao.getPontos(),
        movimentacao.getId(),
        toRedisResponse(movimentacao));
  }

  public SaldoPontuacaoResponse toSaldoResponse(
      Integer usuarioId,
      Long saldo) {
    return new SaldoPontuacaoResponse(usuarioId, saldo);
  }

  public MovimentacaoPontosResponse toMovimentacaoResponse(
      MovimentacaoPontos movimentacao) {
    return new MovimentacaoPontosResponse(
        movimentacao.getId(),
        movimentacao.getUsuarioId(),
        movimentacao.getCondominioId(),
        movimentacao.getTorreId(),
        movimentacao.getCategoriaId(),
        movimentacao.getOrigemTipo(),
        movimentacao.getPostagemId(),
        movimentacao.getTentativaQuizId(),
        movimentacao.getTipoMovimentacao(),
        movimentacao.getPontos(),
        movimentacao.getMovimentacaoReferenciaId(),
        movimentacao.getOcorridoEm());
  }

  public PaginaResponse<MovimentacaoPontosResponse> toPaginaResponse(
      Pagina<MovimentacaoPontos> pagina) {
    var conteudo = pagina.conteudo()
        .stream()
        .map(this::toMovimentacaoResponse)
        .toList();

    return new PaginaResponse<>(
        conteudo,
        pagina.pagina(),
        pagina.tamanho(),
        pagina.totalElementos(),
        pagina.totalPaginas());
  }

  private RedisSincronizacaoResponse toRedisResponse(
      MovimentacaoPontos movimentacao) {
    boolean sincronizado = Boolean.TRUE.equals(movimentacao.getRedisSincronizado());

    return new RedisSincronizacaoResponse(
        sincronizado,
        sincronizado ? "SINCRONIZADO" : "PENDENTE_RETRY");
  }
}