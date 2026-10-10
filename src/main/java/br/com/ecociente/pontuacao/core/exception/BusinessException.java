package br.com.ecociente.pontuacao.core.exception;

public class BusinessException extends DomainException {

  public BusinessException(String codigoErro, String message) {
    super(codigoErro, message);
  }
}