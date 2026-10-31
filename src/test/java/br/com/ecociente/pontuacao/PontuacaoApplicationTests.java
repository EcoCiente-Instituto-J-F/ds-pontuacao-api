package br.com.ecociente.pontuacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import br.com.ecociente.pontuacao.config.job.PostValidationClosingJob;
import br.com.ecociente.pontuacao.config.job.RedisPendingSyncJob;

@Testcontainers
@SpringBootTest(properties = {
        "app.security.jwt.secret=0123456789abcdef0123456789abcdef",
        "app.ranking.cycle-epoch=2020-01-01",
        "app.jobs.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
        "spring.data.redis.repositories.enabled=false"
})
class PontuacaoApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pontuacao_context_test")
            .withUsername("teste")
            .withPassword("teste");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.username", () -> "");
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithoutRunningProductionJobs() {
        assertThat(context.getBeansOfType(RedisPendingSyncJob.class)).isEmpty();
        assertThat(context.getBeansOfType(PostValidationClosingJob.class)).isEmpty();
    }
}
