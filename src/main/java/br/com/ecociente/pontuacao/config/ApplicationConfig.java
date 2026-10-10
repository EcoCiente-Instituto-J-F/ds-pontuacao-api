package br.com.ecociente.pontuacao.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  @Bean
  public Clock clock(
      @Value("${app.timezone:America/Sao_Paulo}") String timezone) {
    return Clock.system(ZoneId.of(timezone));
  }
}