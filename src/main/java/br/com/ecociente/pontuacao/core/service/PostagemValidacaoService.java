
package br.com.ecociente.pontuacao.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.Postagem;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.core.gateway.PostagemGateway;
import br.com.ecociente.pontuacao.core.gateway.PostagemValidacaoGateway;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostagemValidacaoService {

    private final PostagemValidacaoGateway validacaoGateway;
    private final PostagemGateway postagemGateway;
    private final PontuacaoService pontuacaoService;

    @Transactional
    public void encerrar(Integer postagemId) {
        validacaoGateway.encerrarJanela(postagemId);

        Postagem postagem = buscarPostagem(postagemId);

        reconciliar(postagemId, "fechamento");

        validacaoGateway.atualizarTrustScore(
            postagem.getUsuarioId(),
            postagem.getCondominioId()
        );
    }

    @Transactional
    public void decidir(
            Integer postagemId,
            boolean aprovar) {

        validacaoGateway.decidir(postagemId, aprovar);

        Postagem postagem = buscarPostagem(postagemId);

        reconciliar(postagemId, "decisao");

        validacaoGateway.atualizarTrustScore(
            postagem.getUsuarioId(),
            postagem.getCondominioId()
        );
    }

    private Postagem buscarPostagem(Integer postagemId) {
        return postagemGateway
            .buscarComBloqueio(postagemId)
            .orElseThrow(() -> new NotFoundException(
                "POSTAGEM_NAO_ENCONTRADA",
                "Postagem não encontrada"
            ));
    }

    private void reconciliar(
            Integer postagemId,
            String operacao) {

        String chave = operacao + "-postagem-" + postagemId;

        pontuacaoService.reconciliarPostagem(
            postagemId,
            chave
        );
    }
}
