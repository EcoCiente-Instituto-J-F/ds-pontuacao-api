package br.com.ecociente.pontuacao.core.gateway;

public interface VinculoCondominialGateway {

  boolean possuiVinculoAtivo(
      Integer usuarioId,
      Integer condominioId);

  boolean torrePertenceCondominio(
      Integer torreId,
      Integer condominioId);

  boolean ehSindico(
      Integer usuarioId,
      Integer condominioId);

  boolean condominioExiste(Integer condominioId);
}
