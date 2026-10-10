package br.com.ecociente.pontuacao.core.exception;

public abstract class DomainException extends RuntimeException {

  private final String codigoErro;

  protected DomainException(String codigoErro, String message) {
    super(message);
    this.codigoErro = codigoErro;
  }

  public String getCodigoErro() {
    return codigoErro;
  }
}