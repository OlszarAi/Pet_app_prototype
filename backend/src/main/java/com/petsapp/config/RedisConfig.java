package com.petsapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Konfiguracja Redis dla aplikacji PetsApp.
 *
 * <p>Uzywamy {@link StringRedisTemplate} bo wszystkie klucze i wartosci sa stringami (UUID jako
 * member, score jako double w Sorted Set). Dzieki temu unikamy serializacji Java i dane w Redis sa
 * czytelne dla monitorowania (redis-cli ZRANGE feed:public 0 -1 WITHSCORES).
 *
 * <p>Polaczenie jest konfigurowane przez auto-konfiguracje Spring Boot na podstawie
 * spring.data.redis.host/port (application-dev.yml) lub REDIS_URL (produkcja).
 *
 * <p>Adnotacja {@code @ConditionalOnBean} gwarantuje ze bean jest tworzony tylko gdy Redis jest
 * dostepny — chroni przed bledem startu w testach, gdzie Redis jest wylaczony.
 */
@Configuration
public class RedisConfig {

  /**
   * StringRedisTemplate uzywany przez FeedCacheService do operacji na Sorted Sets.
   *
   * <p>Bean tworzony wylacznie gdy {@link RedisConnectionFactory} jest dostepny w kontekscie
   * (tj. gdy Redis jest skonfigurowany i dostepny). W profilu testowym Redis jest wylaczony przez
   * auto-konfiguracje, wiec StringRedisTemplate nie jest tworzony — FeedCacheService jest zamiast
   * tego mockowany przez @MockBean w {@code AbstractIntegrationTest}.
   */
  @Bean
  @ConditionalOnBean(RedisConnectionFactory.class)
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
