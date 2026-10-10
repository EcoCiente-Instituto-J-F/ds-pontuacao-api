package br.com.ecociente.pontuacao.core.exception;

public class NotFoundException extends DomainException {

  public NotFoundException(String codigoErro, String message) {
    super(codigoErro, message);
  }
}