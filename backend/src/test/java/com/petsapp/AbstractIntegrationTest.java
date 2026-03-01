package com.petsapp;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Klasa bazowa dla testow integracyjnych.
 *
 * <p>Uruchamia PostgreSQL przez Testcontainers i konfiguruje DataSource dynamicznie. Redis nie jest
 * wymagany w testach — wylaczony przez profil "test" (application-test.yml). RateLimitService uzywa
 * lokalnej mapy in-memory, wiec Redis nie jest potrzebny w tej fazie testow.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("petsapp_test")
          .withUsername("petsapp_test")
          .withPassword("petsapp_test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }
}
