package br.com.ecociente.pontuacao.entrypoint.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.ecociente.pontuacao.core.exception.AlreadyExistsException;
import br.com.ecociente.pontuacao.core.exception.BusinessException;
import br.com.ecociente.pontuacao.core.exception.InconsistenciaPontuacaoException;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.entrypoint.dto.ErrorResponse;
import br.com.ecociente.pontuacao.entrypoint.dto.ValidationError;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        private ResponseEntity<ErrorResponse> resposta(
                        HttpStatus status,
                        String codigo,
                        String mensagem) {
                var body = new ErrorResponse(
                                status.value(),
                                codigo,
                                List.of(new ValidationError(null, mensagem)));

                return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
                log.warn("Regra de negócio: {}", ex.getCodigoErro());

                return resposta(
                                HttpStatus.CONFLICT,
                                ex.getCodigoErro(),
                                ex.getMessage());
        }

        @ExceptionHandler(AlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleAlreadyExists(
                        AlreadyExistsException ex) {
                return resposta(
                                HttpStatus.CONFLICT,
                                ex.getCodigoErro(),
                                ex.getMessage());
        }

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
                return resposta(
                                HttpStatus.NOT_FOUND,
                                ex.getCodigoErro(),
                                ex.getMessage());
        }

        @ExceptionHandler(InconsistenciaPontuacaoException.class)
        public ResponseEntity<ErrorResponse> handleApi(InconsistenciaPontuacaoException ex) {
                log.error("Inconsistência interna: {}", ex.getCodigoErro(), ex);

                return resposta(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                ex.getCodigoErro(),
                                "Não foi possível concluir a operação");
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(
                        AccessDeniedException ex) {
                return resposta(
                                HttpStatus.FORBIDDEN,
                                "ACESSO_NEGADO",
                                "Você não possui permissão para esta operação");
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleIntegrity(
                        DataIntegrityViolationException ex) {
                log.error("Violação de integridade no banco", ex);

                return resposta(
                                HttpStatus.CONFLICT,
                                "CONFLITO_DADOS",
                                "A operação conflita com os dados existentes");
        }

        @ExceptionHandler(PessimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> handleLock(
                        PessimisticLockingFailureException ex) {
                return resposta(
                                HttpStatus.CONFLICT,
                                "OPERACAO_CONCORRENTE",
                                "Os dados estão sendo alterados. Tente novamente");
        }

        @ExceptionHandler(DataAccessResourceFailureException.class)
        public ResponseEntity<ErrorResponse> handleDatabaseUnavailable(
                        DataAccessResourceFailureException ex) {
                log.error("Falha de acesso ao banco", ex);

                return resposta(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "BANCO_INDISPONIVEL",
                                "O serviço está temporariamente indisponível");
        }

        @Override
        protected ResponseEntity<Object> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex,
                        HttpHeaders headers,
                        HttpStatusCode status,
                        WebRequest request) {
                var details = ex.getBindingResult()
                                .getAllErrors()
                                .stream()
                                .map(error -> {
                                        String campo = error instanceof org.springframework.validation.FieldError fieldError
                                                        ? fieldError.getField()
                                                        : null;

                                        return new ValidationError(
                                                        campo,
                                                        error.getDefaultMessage());
                                })
                                .toList();

                return new ResponseEntity<>(
                                new ErrorResponse(
                                                status.value(),
                                                "ERRO_VALIDACAO",
                                                details),
                                headers,
                                status);
        }

        @Override
        protected ResponseEntity<Object> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException ex,
                        HttpHeaders headers,
                        HttpStatusCode status,
                        WebRequest request) {
                return new ResponseEntity<>(
                                new ErrorResponse(
                                                status.value(),
                                                "JSON_INVALIDO",
                                                List.of(new ValidationError(
                                                                "body",
                                                                "JSON malformado ou com valores inválidos"))),
                                headers,
                                status);
        }

        @Override
        protected ResponseEntity<Object> handleExceptionInternal(
                        Exception ex,
                        Object body,
                        HttpHeaders headers,
                        HttpStatusCode status,
                        WebRequest request) {
                String mensagem = status.is5xxServerError()
                                ? "Erro interno do servidor"
                                : "Requisição inválida. Verifique método, parâmetros e conteúdo";

                if (status.is5xxServerError()) {
                        log.error("Falha no processamento HTTP", ex);
                }

                return new ResponseEntity<>(
                                new ErrorResponse(
                                                status.value(),
                                                "HTTP_" + status.value(),
                                                List.of(new ValidationError(null, mensagem))),
                                headers,
                                status);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
                log.error("Erro inesperado", ex);

                return resposta(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "ERRO_INTERNO",
                                "Erro interno do servidor");
        }
}