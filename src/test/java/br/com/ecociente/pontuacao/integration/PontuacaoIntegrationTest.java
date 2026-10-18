package br.com.ecociente.pontuacao.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import br.com.ecociente.pontuacao.config.ApplicationConfig;
import br.com.ecociente.pontuacao.config.AutorizacaoPontuacao;
import br.com.ecociente.pontuacao.config.SecurityConfig;
import br.com.ecociente.pontuacao.config.job.PostValidationClosingJob;
import br.com.ecociente.pontuacao.config.job.RedisPendingSyncJob;
import br.com.ecociente.pontuacao.core.domain.MovimentacaoPontos;
import br.com.ecociente.pontuacao.core.domain.TipoRankingType;
import br.com.ecociente.pontuacao.core.exception.BusinessException;
import br.com.ecociente.pontuacao.core.gateway.PostagemValidacaoGateway;
import br.com.ecociente.pontuacao.core.gateway.SincronizacaoPontuacaoGateway;
import br.com.ecociente.pontuacao.core.service.CycleService;
import br.com.ecociente.pontuacao.core.service.PontuacaoService;
import br.com.ecociente.pontuacao.core.service.PostagemValidacaoService;
import br.com.ecociente.pontuacao.core.service.ReconstrucaoRankingService;
import br.com.ecociente.pontuacao.core.service.SincronizacaoPontuacaoService;
import br.com.ecociente.pontuacao.dataprovaider.entity.MovimentacaoPontosEntity;
import br.com.ecociente.pontuacao.dataprovaider.repository.MovimentacaoPontosRepository;
import br.com.ecociente.pontuacao.entrypoint.dto.request.EscopoReconstrucaoType;

@Tag("integracao")
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(classes = PontuacaoIntegrationTest.Configuracao.class, properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef",
    "app.timezone=America/Sao_Paulo",
    "app.ranking.cycle-epoch=2020-01-01",
    "app.ranking.retention-days=90",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.open-in-view=false",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false",
    "spring.liquibase.enabled=false",
    "spring.data.redis.repositories.enabled=false",
    "spring.data.redis.username=",
    "spring.data.redis.password=",
    "spring.data.redis.connect-timeout=5s",
    "spring.data.redis.timeout=2s",
})
@AutoConfigureMockMvc
class PontuacaoIntegrationTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("pontuacao_test")
      .withUsername("teste")
      .withPassword("teste");

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void propriedadesRedis(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add(
        "spring.data.redis.port",
        () -> REDIS.getMappedPort(6379));
  }

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbc;

  @Autowired
  private StringRedisTemplate redis;

  @Autowired
  private PontuacaoService pontuacao;

  @Autowired
  private SincronizacaoPontuacaoService sincronizacao;

  @Autowired
  private ReconstrucaoRankingService reconstrucao;

  @Autowired
  private PostagemValidacaoService validacao;

  @Autowired
  private SincronizacaoPontuacaoGateway sincronizacaoGateway;

  @Autowired
  private PostagemValidacaoGateway validacaoGateway;

  @Autowired
  private CycleService ciclos;

  @Autowired
  private Clock clock;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private MockMvc mvc;

  @BeforeEach
  void prepararBancoERedis() throws Exception {
    try (
        Connection connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.execute("DROP SCHEMA public CASCADE");
      statement.execute("CREATE SCHEMA public");

      statement.execute(lerArquivo("sql/ecociente-schema.sql"));
      statement.execute(lerArquivo("sql/pontuacao-dados-test.sql"));
    }

    redis.execute((RedisCallback<Void>) connection -> {
      connection.serverCommands().flushDb();
      return null;
    });
  }

  private String lerArquivo(String caminho) throws Exception {
    try (var input = new ClassPathResource(caminho).getInputStream()) {
      return new String(
          input.readAllBytes(),
          StandardCharsets.UTF_8);
    }
  }

  private int criarPostagem(
      int usuarioId,
      int condominioId,
      Integer torreId,
      int saldo,
      boolean vencida) {
    return jdbc.queryForObject(
        """
            INSERT INTO tb_postagens (
                usuario_id, condominio_id, torre_id, categoria_id,
                url_foto, hash_foto, capturada_em, data_postagem,
                saldo_confianca, data_limite_analise
            )
            VALUES (
                ?, ?, ?, 1,
                'https://teste.com/foto',
                md5(random()::text || clock_timestamp()::text),
                now(), now(), ?,
                CASE WHEN ?
                    THEN now() - INTERVAL '1 minute'
                    ELSE now() + INTERVAL '24 hours'
                END
            )
            RETURNING id_postagem
            """,
        Integer.class,
        usuarioId,
        condominioId,
        torreId,
        saldo,
        vencida);
  }

  private int criarTentativa(
      boolean aprovada,
      boolean concluida,
      Integer condominioId,
      Integer torreId) {
    return jdbc.queryForObject(
        """
            INSERT INTO tb_tentativas_quiz (
                usuario_id, quiz_id, condominio_id, torre_id,
                nota, aprovado, concluido_em
            )
            VALUES (
                1, 1, ?, ?,
                CASE WHEN ? THEN 100 ELSE 0 END,
                ?,
                CASE WHEN ? THEN now() ELSE NULL END
            )
            RETURNING id_tentativa
            """,
        Integer.class,
        condominioId,
        torreId,
        aprovada,
        aprovada,
        concluida);
  }

  private long quantidadeMovimentacoes() {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_movimentacoes_pontos",
        Long.class);
  }

  private long saldoPostagem(int postagemId) {
    return jdbc.queryForObject(
        """
            SELECT COALESCE(SUM(
                CASE
                    WHEN tipo_movimentacao = 'ESTORNO' THEN -pontos
                    ELSE pontos
                END
            ), 0)
            FROM tb_movimentacoes_pontos
            WHERE postagem_id = ?
            """,
        Long.class,
        postagemId);
  }

  private String chave(TipoRankingType tipo, int condominioId) {
    return ciclos.montarChave(
        tipo,
        condominioId,
        ciclos.atual(tipo));
  }

  private void conferirScore(
      TipoRankingType tipo,
      int condominioId,
      int participanteId,
      double esperado) {
    Double score = redis.opsForZSet().score(
        chave(tipo, condominioId),
        Integer.toString(participanteId));

    assertNotNull(score);
    assertEquals(esperado, score.doubleValue());
  }

  private String token(
      int usuarioId,
      String perfil,
      Instant expiracao,
      String segredo) throws Exception {
    var claims = new JWTClaimsSet.Builder()
        .subject("usuario@teste.com")
        .claim("usuarioId", usuarioId)
        .claim("perfil", perfil)
        .expirationTime(Date.from(expiracao))
        .build();

    var jwt = new SignedJWT(
        new JWSHeader(JWSAlgorithm.HS256),
        claims);

    jwt.sign(new MACSigner(
        segredo.getBytes(StandardCharsets.UTF_8)));

    return jwt.serialize();
  }

  private String token(int usuarioId, String perfil) throws Exception {
    return token(
        usuarioId,
        perfil,
        Instant.now().plusSeconds(600),
        SECRET);
  }

  @Test
  void deveConcederPontosRespeitandoSaldoRestanteDoTeto() {
    int p1 = criarPostagem(1, 1, 1, 0, false);
    int p2 = criarPostagem(1, 1, 1, 0, false);
    int p3 = criarPostagem(1, 1, 1, 0, false);
    int p4 = criarPostagem(1, 1, 1, 0, false);

    assertEquals(
        Integer.valueOf(10),
        pontuacao.consolidarPostagem(p1, "credito-1").getPontos());
    assertEquals(
        Integer.valueOf(10),
        pontuacao.consolidarPostagem(p2, "credito-2").getPontos());
    assertEquals(
        Integer.valueOf(5),
        pontuacao.consolidarPostagem(p3, "credito-3").getPontos());

    assertNull(pontuacao.consolidarPostagem(p4, "credito-4"));
    assertEquals(3, quantidadeMovimentacoes());
  }

  @Test
  void duasConsolidacoesConcorrentesDevemCriarUmUnicoCredito()
      throws Exception {

    int postagemId = criarPostagem(1, 1, 1, 0, false);
    var executor = Executors.newFixedThreadPool(2);
    var iniciar = new CountDownLatch(1);

    try {
      var primeira = executor.submit(() -> {
        iniciar.await();
        return pontuacao.consolidarPostagem(
            postagemId, "concorrente-a");
      });

      var segunda = executor.submit(() -> {
        iniciar.await();
        return pontuacao.consolidarPostagem(
            postagemId, "concorrente-b");
      });

      iniciar.countDown();

      MovimentacaoPontos a = primeira.get(15, TimeUnit.SECONDS);
      MovimentacaoPontos b = segunda.get(15, TimeUnit.SECONDS);

      assertEquals(a.getId(), b.getId());
      assertEquals(1, quantidadeMovimentacoes());
      assertEquals(10, saldoPostagem(postagemId));

    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void deveConcederRecompensaDoQuizSemDuplicarTentativa() {
    int tentativaId = criarTentativa(true, true, 1, 1);

    var primeira = pontuacao.consolidarQuiz(
        tentativaId, "quiz-primeira");

    var repetida = pontuacao.consolidarQuiz(
        tentativaId, "quiz-repetida");

    assertEquals(Integer.valueOf(30), primeira.getPontos());
    assertEquals(primeira.getId(), repetida.getId());
    assertEquals(1, quantidadeMovimentacoes());
  }

  @Test
  void quizReprovadoOuIncompletoNaoDeveGerarCredito() {
    int reprovada = criarTentativa(false, true, 1, 1);
    int incompleta = criarTentativa(true, false, 1, 1);

    assertThrows(
        BusinessException.class,
        () -> pontuacao.consolidarQuiz(reprovada, "reprovada"));

    assertThrows(
        BusinessException.class,
        () -> pontuacao.consolidarQuiz(incompleta, "incompleta"));

    assertEquals(0, quantidadeMovimentacoes());
  }

  @Test
  void deveEstornarRestaurarESincronizarSemDuplicarScore() {
    int postagemId = criarPostagem(1, 1, 1, 0, false);

    var credito = pontuacao.consolidarPostagem(
        postagemId, "credito");

    assertTrue(sincronizacao.sincronizar(credito.getId()));

    conferirScore(TipoRankingType.MORADORES, 1, 1, 10);
    conferirScore(TipoRankingType.TORRES, 1, 1, 10);

    jdbc.update(
        """
            UPDATE tb_postagens
            SET pontuacao_ativa = FALSE,
                pontuacao_reconciliacao_pendente = TRUE,
                saldo_confianca = -5
            WHERE id_postagem = ?
            """,
        postagemId);

    var estorno = pontuacao.reconciliarPostagem(
        postagemId, "estorno-1");

    assertNotNull(estorno);
    assertEquals(Integer.valueOf(10), estorno.getPontos());
    assertTrue(sincronizacao.sincronizar(estorno.getId()));

    assertEquals(0, saldoPostagem(postagemId));
    assertNull(redis.opsForZSet().score(
        chave(TipoRankingType.MORADORES, 1), "1"));

    assertNull(pontuacao.reconciliarPostagem(
        postagemId, "mesmo-estado"));

    jdbc.update(
        """
            UPDATE tb_postagens
            SET pontuacao_ativa = TRUE,
                pontuacao_reconciliacao_pendente = TRUE,
                saldo_confianca = 0
            WHERE id_postagem = ?
            """,
        postagemId);

    var restauracao = pontuacao.reconciliarPostagem(
        postagemId, "restauracao-1");

    assertNotNull(restauracao);
    assertEquals(Integer.valueOf(10), restauracao.getPontos());

    assertTrue(sincronizacao.sincronizar(restauracao.getId()));
    assertFalse(sincronizacao.sincronizar(restauracao.getId()));

    conferirScore(TipoRankingType.MORADORES, 1, 1, 10);
    conferirScore(TipoRankingType.TORRES, 1, 1, 10);

    assertEquals(3, quantidadeMovimentacoes());
  }

  @Test
  void quizSemCondominioDeveGerarCreditoPessoalSemRanking() {
    int tentativaId = criarTentativa(true, true, null, null);

    var credito = pontuacao.consolidarQuiz(
        tentativaId, "quiz-pessoal");

    assertTrue(sincronizacao.sincronizar(credito.getId()));
    assertEquals(1, quantidadeMovimentacoes());

    var chaves = redis.keys("ecociente:ranking:*");
    assertNotNull(chaves);
    assertTrue(chaves.isEmpty());
  }

  @Test
  void postagemSemTorreNaoDeveCriarRankingDeTorres() {
    int postagemId = criarPostagem(1, 1, null, 0, false);

    var credito = pontuacao.consolidarPostagem(
        postagemId, "sem-torre");

    sincronizacao.sincronizar(credito.getId());

    conferirScore(TipoRankingType.MORADORES, 1, 1, 10);

    assertFalse(Boolean.TRUE.equals(
        redis.hasKey(chave(TipoRankingType.TORRES, 1))));
  }

  @Test
  void deveIsolarCondominiosESomarSomenteTorreCorrespondente() {
    int p1 = criarPostagem(1, 1, 1, 0, false);
    int p2 = criarPostagem(1, 1, 2, 0, false);
    int p3 = criarPostagem(3, 2, 3, 0, false);

    for (int id : List.of(p1, p2, p3)) {
      var credito = pontuacao.consolidarPostagem(
          id, "isolamento-" + id);
      sincronizacao.sincronizar(credito.getId());
    }

    conferirScore(TipoRankingType.MORADORES, 1, 1, 20);
    conferirScore(TipoRankingType.MORADORES, 2, 3, 10);

    conferirScore(TipoRankingType.TORRES, 1, 1, 10);
    conferirScore(TipoRankingType.TORRES, 1, 2, 10);
    conferirScore(TipoRankingType.TORRES, 2, 3, 10);

    assertNull(redis.opsForZSet().score(
        chave(TipoRankingType.MORADORES, 1), "3"));
  }

  @Test
  void deveReconstruirRankingAPartirDoHistoricoOficial() {
    int postagemId = criarPostagem(1, 1, 1, 0, false);
    pontuacao.consolidarPostagem(postagemId, "rebuild-credito");

    redis.opsForZSet().add(
        chave(TipoRankingType.MORADORES, 1), "999", 999);

    reconstrucao.reconstruir(EscopoReconstrucaoType.TODOS, 1);

    conferirScore(TipoRankingType.MORADORES, 1, 1, 10);
    conferirScore(TipoRankingType.TORRES, 1, 1, 10);

    assertNull(redis.opsForZSet().score(
        chave(TipoRankingType.MORADORES, 1), "999"));

    assertTrue(redis.getExpire(
        chave(TipoRankingType.MORADORES, 1)) > 0);
  }

  @Test
  void rebuildNaoDeveDependerDeChaveSwapAntiga() {
    int postagemId = criarPostagem(1, 1, 1, 0, false);
    pontuacao.consolidarPostagem(postagemId, "swap-credito");

    String chave = chave(TipoRankingType.MORADORES, 1);

    redis.opsForValue().set(chave + ":swap", "residuo-antigo");

    assertDoesNotThrow(() -> reconstrucao.reconstruir(
        EscopoReconstrucaoType.MORADORES, 1));

    conferirScore(TipoRankingType.MORADORES, 1, 1, 10);
  }

  @Test
  void lockDaSincronizacaoDeveImpedirRebuild() throws Exception {
    try (Connection connection = conexaoComLockGlobal()) {
      assertThrows(
          BusinessException.class,
          () -> reconstrucao.reconstruir(
              EscopoReconstrucaoType.MORADORES, 1));

      connection.rollback();
    }

    assertDoesNotThrow(() -> reconstrucao.reconstruir(
        EscopoReconstrucaoType.MORADORES, 1));
  }

  private Connection conexaoComLockGlobal() throws Exception {
    Connection connection = DriverManager.getConnection(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());

    connection.setAutoCommit(false);

    try (var statement = connection.createStatement()) {
      statement.execute(
          "SELECT pg_advisory_xact_lock(731942001)");
    }

    return connection;
  }

  @Test
  void rebuildDeveManterLockAteFinalDaTransacao() throws Exception {
    int postagemId = criarPostagem(1, 1, 1, 0, false);
    var credito = pontuacao.consolidarPostagem(
        postagemId, "lock-rebuild");

    var executor = Executors.newSingleThreadExecutor();
    var transaction = new TransactionTemplate(transactionManager);

    try {
      transaction.executeWithoutResult(status -> {
        reconstrucao.reconstruir(
            EscopoReconstrucaoType.TODOS, 1);

        try {
          boolean sincronizou = executor.submit(
              () -> sincronizacao.sincronizar(credito.getId())).get(10, TimeUnit.SECONDS);

          assertFalse(sincronizou);
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      });

      assertTrue(sincronizacao.sincronizar(credito.getId()));
      conferirScore(TipoRankingType.MORADORES, 1, 1, 10);

    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void falhaNoRedisDeveManterCreditoEPermitirRetry() throws Exception {
    int postagemId = criarPostagem(1, 1, 1, 0, false);

    REDIS.getDockerClient()
        .pauseContainerCmd(REDIS.getContainerId())
        .exec();

    Long movimentacaoId;

    try {
      var credito = pontuacao.consolidarPostagem(
          postagemId, "redis-fora");

      movimentacaoId = credito.getId();

      var job = new RedisPendingSyncJob(
          sincronizacaoGateway,
          sincronizacao,
          clock,
          100);

      job.executar();

      assertEquals(1, quantidadeMovimentacoes());

      assertFalse(Boolean.TRUE.equals(jdbc.queryForObject(
          """
              SELECT redis_sincronizado
              FROM tb_movimentacoes_pontos
              WHERE id_movimentacao = ?
              """,
          Boolean.class,
          movimentacaoId)));

      assertEquals(
          Integer.valueOf(1),
          jdbc.queryForObject(
              """
                  SELECT tentativas_sync_redis
                  FROM tb_movimentacoes_pontos
                  WHERE id_movimentacao = ?
                  """,
              Integer.class,
              movimentacaoId));

    } finally {
      REDIS.getDockerClient()
          .unpauseContainerCmd(REDIS.getContainerId())
          .exec();
    }

    aguardarRedis();

    assertTrue(sincronizacao.sincronizar(movimentacaoId));

    conferirScore(TipoRankingType.MORADORES, 1, 1, 10);
    assertEquals(1, quantidadeMovimentacoes());
  }

  private void aguardarRedis() throws Exception {
    long limite = System.nanoTime()
        + TimeUnit.SECONDS.toNanos(10);

    while (System.nanoTime() < limite) {
      try {
        Boolean respondeu = redis.execute(
            (RedisCallback<Boolean>) connection -> "PONG".equalsIgnoreCase(connection.ping()));

        if (Boolean.TRUE.equals(respondeu)) {
          return;
        }
      } catch (RuntimeException ignored) {
      }

      Thread.sleep(100);
    }

    fail("Redis não voltou a responder após ser retomado");
  }

  @Test
  void selecaoDevePriorizarMovimentacoesMenosTentadas() {
    int p1 = criarPostagem(1, 1, 1, 0, false);
    int p2 = criarPostagem(1, 1, 1, 0, false);

    var primeira = pontuacao.consolidarPostagem(p1, "fila-1");
    var segunda = pontuacao.consolidarPostagem(p2, "fila-2");

    sincronizacaoGateway.registrarFalha(primeira.getId());

    assertEquals(
        List.of(segunda.getId()),
        sincronizacaoGateway.buscarIdsPendentes(1));
  }

  @ParameterizedTest
  @ValueSource(ints = { -3, -4 })
  void fechamentoDeveColocarSaldosIntermediariosEmAnalise(int saldo) {
    int postagemId = criarPostagem(1, 1, 1, saldo, true);

    pontuacao.consolidarPostagem(
        postagemId, "analise-" + postagemId);

    validacao.encerrar(postagemId);

    assertEquals(
        "em_analise",
        jdbc.queryForObject(
            """
                SELECT s.nome_status
                FROM tb_postagens p
                JOIN tb_lkp_status_validacoes_postagens s
                  ON s.id_status_validacao = p.status_validacao_id
                WHERE p.id_postagem = ?
                """,
            String.class,
            postagemId));

    assertNotNull(jdbc.queryForObject(
        "SELECT resolvido_em FROM tb_postagens WHERE id_postagem = ?",
        java.sql.Timestamp.class,
        postagemId));
  }

  @Test
  void fechamentoReprovadoDeveEstornarUmaUnicaVez() {
    int postagemId = criarPostagem(1, 1, 1, -5, true);
    pontuacao.consolidarPostagem(postagemId, "fechamento-credito");

    validacao.encerrar(postagemId);

    assertEquals(0, saldoPostagem(postagemId));
    assertEquals(2, quantidadeMovimentacoes());

    Long auditoriasAntes = jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_log_auditoria",
        Long.class);

    validacao.encerrar(postagemId);

    assertEquals(2, quantidadeMovimentacoes());
    assertEquals(
        auditoriasAntes,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM tb_log_auditoria",
            Long.class));
  }

  @Test
  void outraInstanciaNaoDeveFecharPostagemComLockOcupado()
      throws Exception {

    int postagemId = criarPostagem(1, 1, 1, 0, true);

    try (Connection connection = DriverManager.getConnection(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword())) {
      connection.setAutoCommit(false);

      try (var statement = connection.prepareStatement(
          "SELECT pg_advisory_xact_lock(731942002, ?)")) {
        statement.setInt(1, postagemId);
        statement.execute();
      }

      assertThrows(
          BusinessException.class,
          () -> validacao.encerrar(postagemId));

      connection.rollback();
    }

    assertDoesNotThrow(() -> validacao.encerrar(postagemId));
  }

  @Test
  void jobDeFechamentoDeveAvancarEVoltarAoInicio() {
    int p1 = criarPostagem(1, 1, 1, 0, true);
    int p2 = criarPostagem(1, 1, 1, 0, true);

    var job = new PostValidationClosingJob(
        validacaoGateway, validacao, 1);

    job.executar();
    job.executar();

    assertNotNull(jdbc.queryForObject(
        "SELECT resolvido_em FROM tb_postagens WHERE id_postagem = ?",
        java.sql.Timestamp.class,
        p1));

    assertNotNull(jdbc.queryForObject(
        "SELECT resolvido_em FROM tb_postagens WHERE id_postagem = ?",
        java.sql.Timestamp.class,
        p2));

    job.executar();

    int p3 = criarPostagem(1, 1, 1, 0, true);
    job.executar();

    assertNotNull(jdbc.queryForObject(
        "SELECT resolvido_em FROM tb_postagens WHERE id_postagem = ?",
        java.sql.Timestamp.class,
        p3));
  }

  @Test
  void sindicoResponsavelDeveConsultarRankingSemVinculoRelacional()
      throws Exception {

    mvc.perform(get("/api/v1/rankings/torres")
        .param("condominioId", "1")
        .header("Authorization", "Bearer " + token(2, "SINDICO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ranking").isArray());
  }

  @Test
  void sindicoNaoDeveConsultarCondominioDeOutroResponsavel()
      throws Exception {

    mvc.perform(get("/api/v1/rankings/torres")
        .param("condominioId", "2")
        .header("Authorization", "Bearer " + token(2, "SINDICO")))
        .andExpect(status().isForbidden());
  }

  @Test
  void sindicoDeveDecidirPostagemDoSeuCondominio() throws Exception {
    int postagemId = criarPostagem(1, 1, 1, -3, true);

    pontuacao.consolidarPostagem(postagemId, "decisao-credito");
    validacao.encerrar(postagemId);

    mvc.perform(patch(
        "/api/v1/postagens/{postagemId}/decisao", postagemId)
        .header("Authorization", "Bearer " + token(2, "SINDICO"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"aprovar": true}
            """))
        .andExpect(status().isOk());

    assertEquals(10, saldoPostagem(postagemId));
  }

  @Test
  void usuarioComumNaoDeveAcessarAdministracao() throws Exception {
    mvc.perform(get("/api/v1/admin/redis/status")
        .header("Authorization", "Bearer " + token(1, "USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void consultaSemTokenDeveRetornar401() throws Exception {
    mvc.perform(get("/api/v1/rankings/moradores")
        .param("condominioId", "1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenComOutraAssinaturaDeveRetornar401() throws Exception {
    String invalido = token(
        1,
        "USER",
        Instant.now().plusSeconds(600),
        "abcdef0123456789abcdef0123456789");

    mvc.perform(get("/api/v1/rankings/moradores")
        .param("condominioId", "1")
        .header("Authorization", "Bearer " + invalido))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenExpiradoDeveRetornar401() throws Exception {
    String expirado = token(
        1,
        "USER",
        Instant.now().minusSeconds(600),
        SECRET);

    mvc.perform(get("/api/v1/rankings/moradores")
        .param("condominioId", "1")
        .header("Authorization", "Bearer " + expirado))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void limiteZeroDeveRetornar400ComCorpoDeErro() throws Exception {
    mvc.perform(get("/api/v1/rankings/moradores")
        .param("condominioId", "1")
        .param("limit", "0")
        .header("Authorization", "Bearer " + token(1, "USER")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigoError").value("ERRO_VALIDACAO"));
  }

  @Test
  void chaveDeIdempotenciaEmBrancoDeveRetornar400() throws Exception {
    int postagemId = criarPostagem(1, 1, 1, 0, false);

    mvc.perform(post(
        "/api/v1/pontuacoes/postagens/{id}/consolidar",
        postagemId)
        .header("Authorization", "Bearer " + token(2, "SERVICE"))
        .header("Idempotency-Key", " "))
        .andExpect(status().isBadRequest());

    assertEquals(0, quantidadeMovimentacoes());
  }

  @Test
  void rebuildInvalidoDeveRetornar400ComCorpo() throws Exception {
    mvc.perform(post("/api/v1/admin/rankings/reconstruir")
        .header("Authorization", "Bearer " + token(2, "ADMIN"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
                "tipo": "TODOS",
                "condominioId": 0,
                "ciclo": "ANTIGO"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigoError").value("ERRO_VALIDACAO"));
  }

  @Test
  void clienteNaoDeveEnviarPontosArbitrarios() throws Exception {
    int postagemId = criarPostagem(1, 1, 1, 0, false);

    mvc.perform(post(
        "/api/v1/pontuacoes/postagens/{id}/consolidar",
        postagemId)
        .header("Authorization", "Bearer " + token(2, "SERVICE"))
        .header("Idempotency-Key", "body-proibido")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"pontos": 999999}
            """))
        .andExpect(status().isBadRequest());

    assertEquals(0, quantidadeMovimentacoes());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = MovimentacaoPontosEntity.class)
  @EnableJpaRepositories(basePackageClasses = MovimentacaoPontosRepository.class)
  @ComponentScan(basePackages = {
      "br.com.ecociente.pontuacao.core.service",
      "br.com.ecociente.pontuacao.dataprovaider.gateway",
      "br.com.ecociente.pontuacao.dataprovaider.mapper",
      "br.com.ecociente.pontuacao.entrypoint"
  })
  @Import({
      SecurityConfig.class,
      AutorizacaoPontuacao.class,
      ApplicationConfig.class
  })
  static class Configuracao {
  }
}