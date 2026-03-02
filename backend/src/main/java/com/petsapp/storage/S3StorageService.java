package com.petsapp.storage;

import java.io.InputStream;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Implementacja StorageService oparta na AWS SDK v2.
 *
 * <p>W srodowisku dev wskazuje na lokalny MinIO (S3-compatible API). W prod wskazuje na AWS S3.
 * Endpoint i konfiguracja wstrzykiwane przez StorageProperties — zero hardkodowanych URLi.
 */
@Service
public class S3StorageService implements StorageService {

  private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

  private final S3Client s3Client;
  private final String bucket;
  private final String endpointUrl;

  public S3StorageService(StorageProperties properties) {
    this.bucket = properties.getBucket();
    this.endpointUrl = properties.getEndpoint();

    AwsBasicCredentials credentials =
        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());

    var builder =
        S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(properties.getRegion()));

    // Gdy endpoint jest zdefiniowany (MinIO lub LocalStack), ustawiamy custom URL
    if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
      builder
          .endpointOverride(URI.create(properties.getEndpoint()))
          .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
    }

    this.s3Client = builder.build();
  }

  @Override
  public String upload(
      String key, InputStream inputStream, String contentType, long contentLength) {
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(contentLength)
            .build();

    s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

    String publicUrl = buildPublicUrl(key);
    log.debug("Uploaded file to storage: key={}, url={}", key, publicUrl);
    return publicUrl;
  }

  @Override
  public void delete(String key) {
    DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
    s3Client.deleteObject(request);
    log.debug("Deleted file from storage: key={}", key);
  }

  /**
   * Buduje publiczny URL do zasobu. Dla MinIO format to: {endpoint}/{bucket}/{key} Dla AWS S3
   * format to: https://{bucket}.s3.{region}.amazonaws.com/{key}
   */
  private String buildPublicUrl(String key) {
    if (endpointUrl != null && !endpointUrl.isBlank()) {
      return endpointUrl + "/" + bucket + "/" + key;
    }
    return "https://"
        + bucket
        + ".s3."
        + s3Client.serviceClientConfiguration().region().id()
        + ".amazonaws.com/"
        + key;
  }
}
