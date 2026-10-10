
package br.com.ecociente.pontuacao.entrypoint.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import br.com.ecociente.pontuacao.config.AutorizacaoPontuacao;
import br.com.ecociente.pontuacao.core.domain.Ranking;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;
import br.com.ecociente.pontuacao.core.service.RankingService;
import br.com.ecociente.pontuacao.entrypoint.dto.response.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
@Validated
public class RankingController {

  private final RankingService rankingService;
  private final AutorizacaoPontuacao autorizacaoPontuacao;

  @GetMapping("/moradores")
  public ResponseEntity<RankingResponse<MoradorRankingResponse>> consultarMoradores(
      @RequestParam("condominioId") @Positive Integer condominioId,
      @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(100) int limit,
      Authentication authentication) {

    validarAcessoCondominio(authentication, condominioId);

    Ranking ranking = rankingService.consultar(
        TipoRankingType.MORADORES,
        condominioId,
        limit);

    List<MoradorRankingResponse> moradores = ranking.getParticipantes()
        .stream()
        .map(p -> new MoradorRankingResponse(
            p.getPosicao(),
            p.getParticipanteId(),
            p.getNome(),
            p.getPontos()))
        .toList();

    return ResponseEntity.ok(new RankingResponse<>(
        ranking.getTipo(),
        ranking.getCondominioId(),
        new CicloRankingResponse(
            ranking.getCiclo().getId(),
            ranking.getCiclo().getInicio(),
            ranking.getCiclo().getFim()),
        moradores));
  }

  @GetMapping("/torres")
  public ResponseEntity<RankingResponse<TorreRankingResponse>> consultarTorres(
      @RequestParam("condominioId") @Positive Integer condominioId,
      @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(100) int limit,
      Authentication authentication) {

    validarAcessoCondominio(authentication, condominioId);

    Ranking ranking = rankingService.consultar(
        TipoRankingType.TORRES,
        condominioId,
        limit);

    List<TorreRankingResponse> torres = ranking.getParticipantes()
        .stream()
        .map(p -> new TorreRankingResponse(
            p.getPosicao(),
            p.getParticipanteId(),
            p.getNome(),
            p.getPontos()))
        .toList();

    return ResponseEntity.ok(new RankingResponse<>(
        ranking.getTipo(),
        ranking.getCondominioId(),
        new CicloRankingResponse(
            ranking.getCiclo().getId(),
            ranking.getCiclo().getInicio(),
            ranking.getCiclo().getFim()),
        torres));
  }

  @GetMapping("/moradores/{usuarioId}/posicao")
  public ResponseEntity<PosicaoMoradorResponse> posicaoMorador(
      @PathVariable("usuarioId") @Positive Integer usuarioId,
      @RequestParam("condominioId") @Positive Integer condominioId,
      Authentication authentication) {

    validarAcessoCondominio(authentication, condominioId);

    return ResponseEntity.ok(
        new PosicaoMoradorResponse(
            usuarioId,
            condominioId,
            rankingService.consultarPosicao(
                TipoRankingType.MORADORES,
                condominioId,
                usuarioId),
            rankingService.consultarPontuacao(
                TipoRankingType.MORADORES,
                condominioId,
                usuarioId),
            rankingService.contarParticipantes(
                TipoRankingType.MORADORES,
                condominioId)));
  }

  @GetMapping("/torres/{torreId}/posicao")
  public ResponseEntity<PosicaoTorreResponse> posicaoTorre(
      @PathVariable("torreId") @Positive Integer torreId,
      @RequestParam("condominioId") @Positive Integer condominioId,
      Authentication authentication) {

    if (!autorizacaoPontuacao.podeConsultarTorre(
        authentication,
        condominioId,
        torreId)) {

      throw new AccessDeniedException(
          "Acesso não permitido para esta torre");
    }

    return ResponseEntity.ok(
        new PosicaoTorreResponse(
            torreId,
            condominioId,
            rankingService.consultarPosicao(
                TipoRankingType.TORRES,
                condominioId,
                torreId),
            rankingService.consultarPontuacao(
                TipoRankingType.TORRES,
                condominioId,
                torreId),
            rankingService.contarParticipantes(
                TipoRankingType.TORRES,
                condominioId)));
  }

  private void validarAcessoCondominio(
      Authentication authentication,
      Integer condominioId) {

    if (!autorizacaoPontuacao.podeConsultarCondominio(
        authentication,
        condominioId)) {

      throw new AccessDeniedException(
          "Você não possui vínculo ativo com este condomínio");
    }
  }
}
