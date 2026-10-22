# Contribuindo com a API de Pontuação

Ao participar, você concorda em seguir o [Código de Conduta](CODE_OF_CONDUCT.md). Para reportar um problema de segurança, **não** abra uma issue — siga o [SECURITY.md](SECURITY.md).

## Idioma

O projeto é em português do Brasil: código de domínio, mensagens de erro, commits, issues e Pull Requests.

## Configurando o ambiente

1. Instale o JDK 21 e o Docker.
2. Clone o repositório:
   ```bash
   git clone https://github.com/EcoCiente-Instituto-J-F/ds-pontuacao-api.git
   cd ds-pontuacao-api
   ```
3. Se o seu JDK 21 não estiver em `C:/Program Files/Java/jdk-21.0.12.1`, ajuste `org.gradle.java.home` no `gradle.properties` apenas na sua máquina (não commite essa mudança).
4. Para rodar a aplicação, crie um `.env` com as variáveis listadas no [README](README.md#instalando) e use `./gradlew bootRun`.

## Testando sua mudança

O comando oficial, o mesmo do CI, é:

```bash
./gradlew clean build --no-daemon
```

Regras:

- Toda mudança de comportamento vem com um teste que falha sem ela.
- Testes unitários usam JUnit e Mockito; os de integração usam Testcontainers (PostgreSQL e Redis), por isso o Docker precisa estar ativo.
- Testes não podem depender de rede externa, de API paga nem de dados de produção.

## Regras do projeto

- **Arquitetura em camadas:** regras de negócio em `core/service`, acesso a dados atrás das interfaces de `core/gateway` (implementadas em `dataprovaider/gateway`) e controllers/DTOs em `entrypoint`.
- **Autorização centralizada:** as regras por rota ficam em `config/SecurityConfig.java`, e as regras por usuário/condomínio em `config/AutorizacaoPontuacao.java`. Rota nova precisa ser declarada em `SecurityConfig` (o padrão é `denyAll`) e ter teste de autorização.
- **Schema do banco não é desta API:** `ddl-auto=validate`. Mudanças em entidades exigem o schema correspondente, e o SQL de teste fica em `src/test/resources/sql`.
- **Configuração em um só lugar:** propriedades em `src/main/resources/application.properties`, vindas de variáveis de ambiente.
- **Versão da imagem:** o CI publica `ecociente/ecociente:pontuacao-<versão>` e rejeita tag já existente. Mudanças que vão para a `main` e geram nova imagem precisam subir a `version` no `build.gradle`.
- Nenhum segredo no repositório (tokens, senhas, `.env`).
- Nenhum dado pessoal real em testes ou exemplos.
- Nenhuma dependência nova sem discussão prévia em issue.

## Pull requests

1. Crie sua branch a partir da `main` (ex.: `feat/ranking-por-torre`, `fix/saldo-negativo`).
2. Faça commits focados, com prefixo: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:` ou `ci:`.
3. Abra a PR para a `main`. A descrição é gerada pelo bot do projeto; revise e complete o que for necessário.
4. O CI (build e testes) precisa passar.
5. Pelo menos um mantenedor revisa antes do merge.

## Reportando bugs e propondo funcionalidades

Abra uma issue descrevendo o comportamento esperado, o observado e os passos para reproduzir (com dados fictícios). Para funcionalidades, explique o problema que ela resolve antes de propor a solução.
