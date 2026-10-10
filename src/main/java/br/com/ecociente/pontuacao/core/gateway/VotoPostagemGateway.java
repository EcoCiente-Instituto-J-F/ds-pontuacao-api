package br.com.ecociente.pontuacao.core.gateway;

public interface VotoPostagemGateway {

    void registrar(
        Integer postagemId,
        Integer usuarioId,
        String tipoVoto,
        Integer motivoDenunciaId,
        String comentario
    );
}