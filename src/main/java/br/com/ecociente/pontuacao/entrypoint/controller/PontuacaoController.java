package br.com.ecociente.pontuacao.entrypoint.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.ecociente.pontuacao.core.service.ConsultaPontuacaoService;
import br.com.ecociente.pontuacao.core.service.PontuacaoService;
import br.com.ecociente.pontuacao.entrypoint.dto.response.MovimentacaoPontosResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.PaginaResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.PontuacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.ReconciliacaoPontuacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.response.SaldoPontuacaoResponse;
import br.com.ecociente.pontuacao.entrypoint.mapper.PontuacaoResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/pontuacoes")
@RequiredArgsConstructor
public class PontuacaoController {

  private final PontuacaoService pontuacaoService;
  private final ConsultaPontuacaoService consultaService;
  private final PontuacaoResponseMapper mapper;

  @PostMapping("/postagens/{postagemId}/consolidar")
  public ResponseEntity<PontuacaoResponse> consolidarPostagem(
      @PathVariable("postagemId") @Positive(message = "O ID da postagem deve ser positivo") Integer postagemId,

      @RequestHeader("Idempotency-Key") @NotBlank(message = "A chave de idempotência é obrigatória") @Size(max = 150, message = "A chave deve ter até 150 caracteres") String idempotencyKey,

      @RequestBody(required = false) byte[] body) {
    validarAusenciaDeBody(body);

    var movimentacao = pontuacaoService.consolidarPostagem(
        postagemId,
        idempotencyKey);

    return ResponseEntity.ok(
        mapper.toPostagemResponse(postagemId, movimentacao));
  }

  @PostMapping("/quizzes/tentativas/{tentativaId}/consolidar")
  public ResponseEntity<PontuacaoResponse> consolidarQuiz(
      @PathVariable("tentativaId") @Positive(message = "O ID da tentativa deve ser positivo") Integer tentativaId,

      @RequestHeader("Idempotency-Key") @NotBlank(message = "A chave de idempotência é obrigatória") @Size(max = 150, message = "A chave deve ter até 150 caracteres") String idempotencyKey,

      @RequestBody(required = false) byte[] body) {
    validarAusenciaDeBody(body);

    var movimentacao = pontuacaoService.consolidarQuiz(
        tentativaId,
        idempotencyKey);

    return ResponseEntity.ok(
        mapper.toPontuacaoResponse(movimentacao));
  }

  @PostMapping("/postagens/{postagemId}/reconciliar")
  public ResponseEntity<ReconciliacaoPontuacaoResponse> reconciliarPostagem(
      @PathVariable("postagemId") @Positive(message = "O ID da postagem deve ser positivo") Integer postagemId,

      @RequestHeader("Idempotency-Key") @NotBlank(message = "A chave de idempotência é obrigatória") @Size(max = 150, message = "A chave deve ter até 150 caracteres") String idempotencyKey,

      @RequestBody(required = false) byte[] body) {
    validarAusenciaDeBody(body);

    var movimentacao = pontuacaoService.reconciliarPostagem(
        postagemId,
        idempotencyKey);

    return ResponseEntity.ok(
        mapper.toReconciliacaoResponse(postagemId, movimentacao));
  }

  @GetMapping("/usuarios/{usuarioId}/saldo")
  @PreAuthorize("@autorizacaoPontuacao.podeConsultar(authentication, #p0)")
  public ResponseEntity<SaldoPontuacaoResponse> consultarSaldo(
      @PathVariable("usuarioId") @Positive(message = "O ID do usuário deve ser positivo") Integer usuarioId) {
    Long saldo = consultaService.consultarSaldo(usuarioId);

    return ResponseEntity.ok(
        mapper.toSaldoResponse(usuarioId, saldo));
  }

  @GetMapping("/usuarios/{usuarioId}/movimentacoes")
  @PreAuthorize("@autorizacaoPontuacao.podeConsultar(authentication, #p0)")
  public ResponseEntity<PaginaResponse<MovimentacaoPontosResponse>> listarMovimentacoes(
      @PathVariable("usuarioId") @Positive(message = "O ID do usuário deve ser positivo") Integer usuarioId,

      @RequestParam(name = "pagina", defaultValue = "0") @Min(value = 0, message = "A página deve ser zero ou maior") int pagina,

      @RequestParam(name = "tamanho", defaultValue = "20") @Min(value = 1, message = "O tamanho mínimo é 1") @Max(value = 100, message = "O tamanho máximo é 100") int tamanho) {
    var movimentacoes = consultaService.listarMovimentacoes(
        usuarioId,
        pagina,
        tamanho);

    return ResponseEntity.ok(
        mapper.toPaginaResponse(movimentacoes));
  }

  private void validarAusenciaDeBody(byte[] body) {
    if (body != null && body.length > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Este endpoint não aceita corpo na requisição");
    }
  }
}