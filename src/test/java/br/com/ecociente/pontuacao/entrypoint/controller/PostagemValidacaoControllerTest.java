
package br.com.ecociente.pontuacao.entrypoint.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import br.com.ecociente.pontuacao.config.AutorizacaoPontuacao;
import br.com.ecociente.pontuacao.core.gateway.PostagemGateway;
import br.com.ecociente.pontuacao.core.service.PostagemValidacaoService;

class PostagemValidacaoControllerTest {

    private MockMvc mvc;
    private PostagemValidacaoService service;
    private PostagemGateway postagemGateway;
    private AutorizacaoPontuacao autorizacao;

    @BeforeEach
    void configurar() {
        service = mock(PostagemValidacaoService.class);
        postagemGateway = mock(PostagemGateway.class);
        autorizacao = mock(AutorizacaoPontuacao.class);

        PostagemValidacaoController controller =
            new PostagemValidacaoController(
                service,
                postagemGateway,
                autorizacao
            );

        LocalValidatorFactoryBean validator =
            new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setValidator(validator)
            .build();
    }

    @Test
    void deveFinalizarValidacao() throws Exception {
        mvc.perform(post(
                "/api/v1/postagens/10/finalizar-validacao"))
            .andExpect(status().isOk());

        verify(service).encerrar(10);
    }

    @Test
    void deveAprovarPostagemEmAnalise() throws Exception {
        when(postagemGateway.buscarCondominioId(10))
            .thenReturn(Optional.of(7));

        when(autorizacao.podeDecidirPostagem(
            any(), eq(7)
        )).thenReturn(true);

        mvc.perform(patch("/api/v1/postagens/10/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "aprovar": true
                    }
                    """))
            .andExpect(status().isOk());

        verify(service).decidir(10, true);
    }

    @Test
    void deveReprovarPostagemEmAnalise() throws Exception {
        when(postagemGateway.buscarCondominioId(10))
            .thenReturn(Optional.of(7));

        when(autorizacao.podeDecidirPostagem(
            any(), eq(7)
        )).thenReturn(true);

        mvc.perform(patch("/api/v1/postagens/10/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "aprovar": false
                    }
                    """))
            .andExpect(status().isOk());

        verify(service).decidir(10, false);
    }

    @Test
    void deveRejeitarDecisaoSemCampoAprovar() throws Exception {
        mvc.perform(patch("/api/v1/postagens/10/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarDecisaoComBodyInvalido() throws Exception {
        mvc.perform(patch("/api/v1/postagens/10/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{json-invalido"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
