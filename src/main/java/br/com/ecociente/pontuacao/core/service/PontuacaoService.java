package br.com.ecociente.pontuacao.core.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PontuacaoService {

  private final PostagemGateway postagemGateway;
  private final CategoriaResiduoGateway categoriaGateway;
  private final QuizGateway quizGateway;
  private final TentativaQuizGateway tentativaGateway;
  private final MovimentacaoPontosGateway movimentacaoGateway;
  private final Clock clock;

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public MovimentacaoPontos consolidarPostagem(
      Integer postagemId,
      String idempotencyKey) {
    validarChave(idempotencyKey);

    Postagem postagem = bloquearPostagem(postagemId);

    validarChaveConsolidacao(
        idempotencyKey,
        OrigemPontuacaoType.POSTAGEM,
        postagemId);

    var creditoExistente = movimentacaoGateway.buscarCreditoPostagem(postagemId);

    if (creditoExistente.isPresent()) {
      return creditoExistente.get();
    }

    if (!Boolean.TRUE.equals(postagem.getPontuacaoAtiva())) {
      throw new BusinessException(
          "POSTAGEM_SEM_PONTUACAO_ATIVA",
          "A postagem não está habilitada para receber pontos");
    }

    Integer pontos = categoriaGateway.calcularPontosDisponiveis(
        postagem.getUsuarioId(),
        postagem.getCategoriaId(),
        postagem.getDataPostagem(),
        clock.getZone().getId());

    if (pontos == null || pontos < 0) {
      throw new InconsistenciaPontuacaoException(
          "PONTUACAO_INVALIDA",
          "O cálculo de pontuação retornou um valor inválido");
    }

    if (pontos == 0) {
      return null;
    }

    MovimentacaoPontos credito = MovimentacaoPontos.builder()
        .usuarioId(postagem.getUsuarioId())
        .condominioId(postagem.getCondominioId())
        .torreId(postagem.getTorreId())
        .categoriaId(postagem.getCategoriaId())
        .origemTipo(OrigemPontuacaoType.POSTAGEM)
        .postagemId(postagem.getId())
        .tipoMovimentacao(TipoMovimentacaoType.CREDITO)
        .pontos(pontos)
        .idempotencyKey(idempotencyKey)
        .ocorridoEm(OffsetDateTime.now(clock))
        .redisSincronizado(false)
        .tentativasSyncRedis(0)
        .build();

    MovimentacaoPontos movimentacao = movimentacaoGateway.criar(credito);

    postagemGateway.marcarReconciliacaoConcluida(postagemId);

    return movimentacao;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public MovimentacaoPontos consolidarQuiz(
      Integer tentativaId,
      String idempotencyKey) {
    validarChave(idempotencyKey);

    TentativaQuiz tentativa = tentativaGateway
        .buscarComBloqueio(tentativaId)
        .orElseThrow(() -> new NotFoundException(
            "TENTATIVA_NAO_ENCONTRADA",
            "Tentativa de quiz não encontrada"));

    validarChaveConsolidacao(
        idempotencyKey,
        OrigemPontuacaoType.QUIZ,
        tentativaId);

    var creditoExistente = movimentacaoGateway.buscarCreditoTentativa(tentativaId);

    if (creditoExistente.isPresent()) {
      return creditoExistente.get();
    }

    if (tentativa.getConcluidoEm() == null) {
      throw new BusinessException(
          "TENTATIVA_NAO_CONCLUIDA",
          "A tentativa ainda não foi concluída");
    }

    if (!Boolean.TRUE.equals(tentativa.getAprovado())) {
      throw new BusinessException(
          "QUIZ_NAO_APROVADO",
          "A tentativa não foi aprovada");
    }

    Quiz quiz = quizGateway.buscarPorId(tentativa.getQuizId())
        .orElseThrow(() -> new NotFoundException(
            "QUIZ_NAO_ENCONTRADO",
            "Quiz não encontrado"));

    if (!Boolean.TRUE.equals(quiz.getAtivo())) {
      throw new BusinessException(
          "QUIZ_INATIVO",
          "O quiz está inativo");
    }

    if (quiz.getPontosRecompensa() == null
        || quiz.getPontosRecompensa() <= 0) {
      throw new InconsistenciaPontuacaoException(
          "RECOMPENSA_INVALIDA",
          "O quiz possui uma recompensa inválida");
    }

    if (tentativa.getTorreId() != null
        && tentativa.getCondominioId() == null) {
      throw new BusinessException(
          "CONTEXTO_CONDOMINIAL_INVALIDO",
          "Uma tentativa vinculada à torre precisa de condomínio");
    }

    MovimentacaoPontos credito = MovimentacaoPontos.builder()
        .usuarioId(tentativa.getUsuarioId())
        .condominioId(tentativa.getCondominioId())
        .torreId(tentativa.getTorreId())
        .origemTipo(OrigemPontuacaoType.QUIZ)
        .tentativaQuizId(tentativa.getId())
        .tipoMovimentacao(TipoMovimentacaoType.CREDITO)
        .pontos(quiz.getPontosRecompensa())
        .idempotencyKey(idempotencyKey)
        .ocorridoEm(OffsetDateTime.now(clock))
        .redisSincronizado(false)
        .tentativasSyncRedis(0)
        .build();

    return movimentacaoGateway.criar(credito);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public MovimentacaoPontos reconciliarPostagem(
      Integer postagemId,
      String idempotencyKey) {
    validarChave(idempotencyKey);

    Postagem postagem = bloquearPostagem(postagemId);

    var movimentacaoExistente = movimentacaoGateway.buscarPorIdempotencyKey(idempotencyKey);

    if (movimentacaoExistente.isPresent()) {
      MovimentacaoPontos existente = movimentacaoExistente.get();

      if (existente.getOrigemTipo() != OrigemPontuacaoType.POSTAGEM
          || !postagemId.equals(existente.getPostagemId())
          || existente.getTipoMovimentacao() == TipoMovimentacaoType.CREDITO) {
        throw conflitoChave();
      }

      return existente;
    }

    long saldo = movimentacaoGateway.calcularSaldoPostagem(postagemId);

    var creditoExistente = movimentacaoGateway.buscarCreditoPostagem(postagemId);

    if (creditoExistente.isEmpty()) {
      if (saldo != 0) {
        throw inconsistencia();
      }

      return null;
    }

    MovimentacaoPontos credito = creditoExistente.get();

    if (saldo != 0 && saldo != credito.getPontos().longValue()) {
      throw inconsistencia();
    }

    if (postagem.getPontuacaoAtiva() == null) {
      throw inconsistencia();
    }

    boolean devePontuar = postagem.getPontuacaoAtiva();

    if ((devePontuar && saldo > 0)
        || (!devePontuar && saldo == 0)) {
      postagemGateway.marcarReconciliacaoConcluida(postagemId);
      return null;
    }

    TipoMovimentacaoType tipo = devePontuar
        ? TipoMovimentacaoType.RESTAURACAO
        : TipoMovimentacaoType.ESTORNO;

    MovimentacaoPontos ajuste = MovimentacaoPontos.builder()
        .usuarioId(credito.getUsuarioId())
        .condominioId(credito.getCondominioId())
        .torreId(credito.getTorreId())
        .categoriaId(credito.getCategoriaId())
        .origemTipo(credito.getOrigemTipo())
        .postagemId(credito.getPostagemId())
        .tipoMovimentacao(tipo)
        .pontos(credito.getPontos())
        .movimentacaoReferenciaId(credito.getId())
        .idempotencyKey(idempotencyKey)
        .ocorridoEm(OffsetDateTime.now(clock))
        .redisSincronizado(false)
        .tentativasSyncRedis(0)
        .build();

    MovimentacaoPontos movimentacao = movimentacaoGateway.criar(ajuste);

    postagemGateway.marcarReconciliacaoConcluida(postagemId);

    return movimentacao;
  }

  private Postagem bloquearPostagem(Integer postagemId) {
    Integer usuarioId = postagemGateway.buscarUsuarioId(postagemId)
        .orElseThrow(() -> new NotFoundException(
            "POSTAGEM_NAO_ENCONTRADA",
            "Postagem não encontrada"));

    if (!postagemGateway.bloquearUsuario(usuarioId)) {
      throw new NotFoundException(
          "USUARIO_NAO_ENCONTRADO",
          "Usuário da postagem não encontrado");
    }

    Postagem postagem = postagemGateway.buscarComBloqueio(postagemId)
        .orElseThrow(() -> new NotFoundException(
            "POSTAGEM_NAO_ENCONTRADA",
            "Postagem não encontrada"));

    if (!usuarioId.equals(postagem.getUsuarioId())) {
      throw new BusinessException(
          "POSTAGEM_ALTERADA",
          "A postagem foi alterada. Tente novamente");
    }

    return postagem;
  }

  private void validarChaveConsolidacao(
      String chave,
      OrigemPontuacaoType origem,
      Integer origemId) {
    movimentacaoGateway.buscarPorIdempotencyKey(chave)
        .ifPresent(existente -> {
          Integer idExistente = origem == OrigemPontuacaoType.POSTAGEM
              ? existente.getPostagemId()
              : existente.getTentativaQuizId();

          if (existente.getOrigemTipo() != origem
              || existente.getTipoMovimentacao() != TipoMovimentacaoType.CREDITO
              || !origemId.equals(idExistente)) {
            throw conflitoChave();
          }
        });
  }

  private void validarChave(String chave) {
    if (chave == null || chave.isBlank() || chave.length() > 150) {
      throw new BusinessException(
          "IDEMPOTENCY_KEY_INVALIDA",
          "Informe uma chave de idempotência com até 150 caracteres");
    }
  }

  private AlreadyExistsException conflitoChave() {
    return new AlreadyExistsException(
        "IDEMPOTENCY_KEY_EM_USO",
        "A chave de idempotência já foi utilizada em outra operação");
  }

  private InconsistenciaPontuacaoException inconsistencia() {
    return new InconsistenciaPontuacaoException(
        "INCONSISTENCIA_MOVIMENTACAO",
        "O saldo da postagem não corresponde ao crédito original");
  }
}