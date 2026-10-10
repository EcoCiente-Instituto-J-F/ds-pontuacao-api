package br.com.ecociente.pontuacao.core.exception;

public class AlreadyExistsException extends DomainException {

    public AlreadyExistsException(String codigoErro, String message) {
        super(codigoErro, message);
    }
}