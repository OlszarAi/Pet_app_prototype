package com.petsapp.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfiguracja magazynu plikow — dev: MinIO, prod: AWS S3.
 *
 * <p>Wszystkie wartosci sa wstrzykiwane z application-dev.yml lub prod przez zmienne env. Nigdy nie
 * hardkodujemy kluczy dostepu w kodzie.
 */
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

  private String type = "minio";
  private String endpoint;
  private String bucket;
  private String accessKey;
  private String secretKey;
  private String region = "us-east-1";

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
