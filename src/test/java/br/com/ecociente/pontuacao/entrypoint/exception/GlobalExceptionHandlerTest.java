package br.com.ecociente.pontuacao.entrypoint.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import br.com.ecociente.pontuacao.core.exception.AlreadyExistsException;
import br.com.ecociente.pontuacao.core.exception.BusinessException;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.entrypoint.dto.ErrorResponse;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  @DisplayName("Deve retornar 409 para conflito de regra de negócio")
  void shouldReturnConflictForBusinessException() {
    var ex = new BusinessException(
        "QUIZ_NAO_APROVADO",
        "A tentativa não foi aprovada");

    var response = handler.handleBusiness(ex);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(409, response.getBody().status());
    assertEquals("QUIZ_NAO_APROVADO", response.getBody().codigoError());
    assertEquals(
        "A tentativa não foi aprovada",
        response.getBody().details().get(0).message());
  }

  @Test
  @DisplayName("Deve retornar 409 para chave já utilizada")
  void shouldReturnConflictForAlreadyExistsException() {
    var ex = new AlreadyExistsException(
        "IDEMPOTENCY_KEY_EM_USO",
        "A chave já foi utilizada");

    var response = handler.handleAlreadyExists(ex);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(409, response.getBody().status());
    assertEquals(
        "IDEMPOTENCY_KEY_EM_USO",
        response.getBody().codigoError());
  }

  @Test
  @DisplayName("Deve retornar 404 para postagem inexistente")
  void shouldReturnNotFoundForMissingPost() {
    var ex = new NotFoundException(
        "POSTAGEM_NAO_ENCONTRADA",
        "Postagem não encontrada");

    var response = handler.handleNotFound(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(
        "POSTAGEM_NAO_ENCONTRADA",
        response.getBody().codigoError());
  }

  @Test
  @DisplayName("Deve retornar 403 quando o acesso for negado")
  void shouldReturnForbiddenForAccessDenied() {
    var ex = new AccessDeniedException("Acesso negado");

    var response = handler.handleAccessDenied(ex);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("ACESSO_NEGADO", response.getBody().codigoError());
  }

  @Test
  @DisplayName("Deve retornar 503 sem expor detalhes da conexão")
  void shouldReturnServiceUnavailableWithoutExposingConnectionDetails() {
    var ex = new DataAccessResourceFailureException(
        "Detalhes internos da conexão");

    var response = handler.handleDatabaseUnavailable(ex);

    assertEquals(
        HttpStatus.SERVICE_UNAVAILABLE,
        response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(
        "O serviço está temporariamente indisponível",
        response.getBody().details().get(0).message());
  }

  @Test
  @DisplayName("Deve retornar os campos inválidos no erro de validação")
  void shouldReturnFieldValidationErrors() throws Exception {
    Method method = GlobalExceptionHandlerTest.class
        .getDeclaredMethod("sampleEndpoint", String.class);

    var parameter = new MethodParameter(method, 0);
    BindingResult bindingResult = mock(BindingResult.class);

    when(bindingResult.getAllErrors()).thenReturn(List.of(
        new FieldError(
            "request",
            "condominioId",
            "O condomínio é obrigatório"),
        new FieldError(
            "request",
            "ciclo",
            "O ciclo é obrigatório")));

    var ex = new MethodArgumentNotValidException(
        parameter,
        bindingResult);

    var response = handler.handleMethodArgumentNotValid(
        ex,
        HttpHeaders.EMPTY,
        HttpStatus.BAD_REQUEST,
        null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    ErrorResponse body = assertInstanceOf(
        ErrorResponse.class,
        response.getBody());

    assertEquals("ERRO_VALIDACAO", body.codigoError());
    assertEquals(2, body.details().size());
    assertEquals("condominioId", body.details().get(0).field());
    assertEquals(
        "O condomínio é obrigatório",
        body.details().get(0).message());
  }

  @Test
  @DisplayName("Deve retornar 400 para JSON inválido")
  void shouldReturnBadRequestForInvalidJson() {
    var input = new MockHttpInputMessage(new byte[0]);

    var ex = new HttpMessageNotReadableException(
        "Detalhe interno do parser",
        input);

    var response = handler.handleHttpMessageNotReadable(
        ex,
        HttpHeaders.EMPTY,
        HttpStatus.BAD_REQUEST,
        null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    ErrorResponse body = assertInstanceOf(
        ErrorResponse.class,
        response.getBody());

    assertEquals("JSON_INVALIDO", body.codigoError());
    assertEquals("body", body.details().get(0).field());
    assertEquals(
        "JSON malformado ou com valores inválidos",
        body.details().get(0).message());
  }

  @Test
  @DisplayName("Deve retornar 500 sem expor a mensagem interna")
  void shouldReturnInternalServerErrorWithoutLeakingDetails() {
    var ex = new RuntimeException("Informação sensível do banco");

    var response = handler.handleUnexpected(ex);

    assertEquals(
        HttpStatus.INTERNAL_SERVER_ERROR,
        response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("ERRO_INTERNO", response.getBody().codigoError());
    assertEquals(
        "Erro interno do servidor",
        response.getBody().details().get(0).message());
  }

  private void sampleEndpoint(String value) {
  }
}