package com.petsapp.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguracja puli watkow dla operacji asynchronicznych (@Async).
 *
 * <p>Pula watkow jest oddzielona od puli HTTP Tomcat — dlugotrwale operacje
 * (push notifications, achievement checking) nie blokuja watkow obslugujacych requesty.
 *
 * <p>Parametry puli:
 * <ul>
 *   <li>corePoolSize=5 — min watkow zawsze aktywnych
 *   <li>maxPoolSize=20 — max watkow przy przeciazeniu
 *   <li>queueCapacity=100 — kolejka zadan przed odrzuceniem
 * </ul>
 */
@Configuration
public class AsyncConfig {

  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor =
        new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("petsapp-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }
}
