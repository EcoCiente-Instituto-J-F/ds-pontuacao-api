package br.com.ecociente.pontuacao.entrypoint.dto.response;

import java.util.List;

public record PaginaResponse<T>(
    List<T> conteudo,
    int pagina,
    int tamanho,
    long totalElementos,
    int totalPaginas
) {
}