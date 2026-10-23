# Política de Segurança

A API de Pontuação lê dados de usuários, condomínios, torres, postagens e tentativas de quiz do EcoCiente (PostgreSQL), grava movimentações de pontos e mantém rankings de moradores e torres no Redis. O acesso é protegido por tokens JWT e por regras de perfil e de vínculo com o condomínio.

## Versões suportadas

| Versão | Suporte |
| --- | --- |
| `main` (mais recente) | Sim |
| Demais branches e tags | Não |

## Reportando uma vulnerabilidade

**Não abra issue, Pull Request ou discussão pública para vulnerabilidades.**

Use o relato privado do GitHub: <https://github.com/EcoCiente-Instituto-J-F/ds-pontuacao-api/security/advisories/new> (aba **Security** → **Advisories** → **Report a vulnerability**).

> Mantenedores: ative o *Private vulnerability reporting* em **Settings → Code security** para que o link acima funcione.

Inclua no relato:

- o impacto (o que um atacante consegue fazer ou ver);
- os passos para reproduzir, usando **dados falsos**;
- o commit testado e a configuração usada;
- se tiver, uma ideia de correção.

O que acontece depois: confirmamos o recebimento, corrigimos de forma privada e publicamos um advisory, com crédito a quem reportou (se desejar). Pedimos um prazo razoável para a correção antes de qualquer divulgação pública.

## Escopo

**Dentro do escopo:**

- acesso a pontuação, movimentações ou rankings de outro usuário ou condomínio;
- burla de autenticação ou autorização (JWT, perfis `ADMIN`, `SINDICO`, `SERVICE`, regras de vínculo);
- aceitação de tokens sem expiração, com assinatura inválida ou segredo fraco;
- injeção (SQL, comandos) com exposição ou alteração de dados;
- vazamento de dados pessoais ou segredos em logs, respostas de erro ou endpoints de diagnóstico;
- segredos commitados no repositório;
- abuso da idempotência ou das rotas administrativas para fraudar pontos.

**Fora do escopo:**

- bugs sem impacto de segurança;
- falhas em serviços de terceiros (PostgreSQL, Redis, Docker Hub, GitHub);
- credenciais de desenvolvimento e dados fictícios de teste;
- negação de serviço contra o seu próprio deploy.

## Como o sistema protege os dados

| Camada | Onde | O que faz |
| --- | --- | --- |
| Autenticação | `config/SecurityConfig.java` | Valida JWT assinado com HMAC (HS256/384/512 conforme o tamanho do segredo), exige segredo de 32+ bytes e claim de expiração; sessão stateless |
| Autorização por rota | `config/SecurityConfig.java` | Define os perfis por rota; qualquer rota não listada é negada (`denyAll`) |
| Autorização por recurso | `config/AutorizacaoPontuacao.java` | Usuário só consulta a própria pontuação; condomínio e torre exigem vínculo ativo ou ser síndico do condomínio; `ADMIN` tem acesso total |
| Método | `@PreAuthorize` nos controllers | Reforça as regras de perfil e de usuário nas rotas |
| Erros | `SecurityConfig` e `entrypoint/exception/GlobalExceptionHandler.java` | Respostas padronizadas (401/403) sem detalhes internos |
| Execução | `Dockerfile` | Container roda com usuário sem privilégios |
| Banco | `application.properties` | `ddl-auto=validate`: a API não altera o schema |

## Checklist de deploy

- Defina `JWT_SECRET` forte (de preferência 64+ bytes) e rotacione-o se houver suspeita de vazamento.
- Mantenha segredos (`JWT_SECRET`, `DB_PASSWORD`, `REDIS_PASSWORD`) fora de ConfigMap, imagem e repositório.
- Use uma role de banco com o mínimo de permissão necessária.
- Use usuário e senha no Redis e não o exponha publicamente.
- Exponha apenas o necessário: `/actuator/health` é público; mantenha `/actuator/metrics` e demais endpoints restritos à rede interna.
- Considere desabilitar o Swagger (`/swagger-ui/**`, `/v3/api-docs/**`, hoje públicos) em produção.
