package br.com.ecociente.pontuacao.core.exception;

public class InconsistenciaPontuacaoException extends DomainException {

  public InconsistenciaPontuacaoException(String codigoErro, String message) {
    super(codigoErro, message);
  }
}