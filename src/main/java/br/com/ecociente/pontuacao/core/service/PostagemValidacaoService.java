package br.com.ecociente.pontuacao.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.pontuacao.core.domain.Postagem;
import br.com.ecociente.pontuacao.core.exception.BusinessException;
import br.com.ecociente.pontuacao.core.exception.NotFoundException;
import br.com.ecociente.pontuacao.core.gateway.PostagemGateway;
import br.com.ecociente.pontuacao.core.gateway.PostagemValidacaoGateway;

@Service
public class PostagemValidacaoService {

    private final PostagemValidacaoGateway validacaoGateway;
    private final PostagemGateway postagemGateway;
    private final PontuacaoService pontuacaoService;

    public PostagemValidacaoService(
            PostagemValidacaoGateway validacaoGateway,
            PostagemGateway postagemGateway,
            PontuacaoService pontuacaoService) {
        this.validacaoGateway = validacaoGateway;
        this.postagemGateway = postagemGateway;
        this.pontuacaoService = pontuacaoService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void encerrar(Integer postagemId) {
        if (!validacaoGateway.tentarBloquearFechamento(postagemId)) {
            throw new BusinessException(
                    "FECHAMENTO_EM_PROCESSAMENTO",
                    "O fechamento desta postagem já está em processamento");
        }

        Postagem postagem = bloquearPostagem(postagemId);

        if (postagem.getResolvidoEm() != null) {
            return;
        }

        if (!validacaoGateway.janelaVencida(postagemId)) {
            throw new BusinessException(
                    "JANELA_AINDA_ABERTA",
                    "A janela comunitária desta postagem ainda não terminou");
        }

        validacaoGateway.encerrarJanela(postagemId);

        reconciliar(postagemId, "fechamento");

        validacaoGateway.atualizarTrustScore(
                postagem.getUsuarioId(),
                postagem.getCondominioId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void decidir(
            Integer postagemId,
            boolean aprovar) {
        Postagem postagem = bloquearPostagem(postagemId);

        validacaoGateway.decidir(postagemId, aprovar);

        reconciliar(postagemId, "decisao");

        validacaoGateway.atualizarTrustScore(
                postagem.getUsuarioId(),
                postagem.getCondominioId());
    }

    private Postagem bloquearPostagem(Integer postagemId) {
        Integer usuarioId = postagemGateway.buscarUsuarioId(postagemId)
                .orElseThrow(() -> new NotFoundException(
                        "POSTAGEM_NAO_ENCONTRADA",
                        "Postagem não encontrada"));

        if (!postagemGateway.bloquearUsuario(usuarioId)) {
            throw new NotFoundException(
                    "USUARIO_NAO_ENCONTRADO",
                    "Usuário da postagem não encontrado");
        }

        Postagem postagem = postagemGateway.buscarComBloqueio(postagemId)
                .orElseThrow(() -> new NotFoundException(
                        "POSTAGEM_NAO_ENCONTRADA",
                        "Postagem não encontrada"));

        if (!usuarioId.equals(postagem.getUsuarioId())) {
            throw new BusinessException(
                    "POSTAGEM_ALTERADA",
                    "A postagem foi alterada. Tente novamente");
        }

        return postagem;
    }

    private void reconciliar(
            Integer postagemId,
            String operacao) {
        pontuacaoService.reconciliarPostagem(
                postagemId,
                operacao + "-postagem-" + postagemId);
    }

}