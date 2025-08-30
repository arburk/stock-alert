package com.github.arburk.stockalert.infrastructure.persistance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.domain.StockAlertDb;
import com.github.arburk.stockalert.application.service.stock.PersistenceProvider;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(value = "stock-alert.storage-provider", havingValue = S3BucketStorage.ENABLE_PROPERTY)
public class S3BucketStorage extends AbstractPersistenceProvider implements PersistenceProvider {

  public static final String ENABLE_PROPERTY = "s3";

  @Value("${spring.cloud.s3.endpoint.url}")
  private String endpoint;
  @Value("${spring.cloud.s3.endpoint.force-path-style}")
  private boolean forcePathStyle;
  @Value("${spring.cloud.s3.credentials.access-key}")
  private String accessKey;
  @Value("${spring.cloud.s3.credentials.secret-key}")
  private String secretKey;
  @Value("${spring.cloud.s3.region}")
  private String region;
  @Value("${spring.cloud.s3.bucket}")
  private String bucket;

  private final ObjectMapper objectMapper;

  private S3Client s3;

  public S3BucketStorage(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    log.debug("Initialized S3BucketStorage as PersistanceProvider");
  }

  @PreDestroy
  public void shutdown() {
    resetS3ClientToEnforceRefresh();
  }

  @Override
  public void commitChanges() {
    try {
      final StringWriter jsonWriter = new StringWriter();
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonWriter, getData());
      final byte[] resultAsBytes = jsonWriter.toString().getBytes(StandardCharsets.UTF_8);
      log.debug("serialized data of lenth {}", resultAsBytes.length);

      try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(resultAsBytes)) {
        final var req = PutObjectRequest.builder().bucket(bucket).key(STORAGE_FILE_NAME).build();
        final var putResponse = getS3().putObject(req, RequestBody.fromInputStream(byteArrayInputStream, resultAsBytes.length));
        log.debug("Put {} to S3 bucket {}: {}", STORAGE_FILE_NAME, bucket, putResponse);
        log.info("Securities successfully updated in {}/{}/{}.}", endpoint, bucket, STORAGE_FILE_NAME);
      }
    } catch (Exception e) {
      log.error("Failed to write securities to {}/{}/{}.", endpoint, bucket, STORAGE_FILE_NAME, e);
      resetS3ClientToEnforceRefresh();
    }
  }

  @Override
  StockAlertDb initData() {
    try {
      final ListObjectsRequest req = ListObjectsRequest.builder()
          .bucket(bucket).prefix(STORAGE_FILE_NAME).build();
      log.debug("get {} from S3 bucket: {}",STORAGE_FILE_NAME, req);
      final List<S3Object> s3Contents = getS3().listObjects(req)
          .contents()
          .stream()
          .toList();


      if (s3Contents.isEmpty()) {
        log.warn("Storage file not found in S3 bucket: {}/{}", bucket, endpoint);
        return initDataByFallback();
      }

      if (s3Contents.size() > 1) {
        log.warn("{} Storage files '{}' found in S3 bucket: {}/{}. Will use first one",
            s3Contents.size(), STORAGE_FILE_NAME, endpoint, bucket);
      }

      try (var responseInputStream = getS3().getObject(GetObjectRequest.builder().bucket(bucket).key(STORAGE_FILE_NAME).build())) {
        return objectMapper.readValue(responseInputStream, new TypeReference<>() {
        });
      }

    } catch (Exception e) {
      log.error("Failed to read securities from S3 bucket.", e);
      resetS3ClientToEnforceRefresh();
      return new StockAlertDb(new ArrayList<>(/* must not be immutable */), null);
    }
  }

  private StockAlertDb initDataByFallback() throws IOException {
    final ListObjectsRequest req = ListObjectsRequest.builder()
        .bucket(bucket).prefix(STORAGE_FILE_NAME_0_1_3).build();
    final List<S3Object> s3Contents = getS3().listObjects(req)
        .contents()
        .stream()
        .toList();

    if (!s3Contents.isEmpty()) {
      log.info("init data from former storage file for migration: {}", STORAGE_FILE_NAME_0_1_3);
      try (var responseInputStream = getS3().getObject(GetObjectRequest.builder().bucket(bucket).key(STORAGE_FILE_NAME_0_1_3).build())) {
        return new StockAlertDb(objectMapper.readValue(responseInputStream, new TypeReference<>() {
        }), null);
      }
    }
    return new StockAlertDb(new ArrayList<>(/* must not be immutable */), null);
  }

  private S3Client getS3() {
    if (s3 == null) {
      log.debug("Configure S3 bucket {}", endpoint);

      final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
      final var awsCredentialsProvider = StaticCredentialsProvider.create(credentials);
      final S3ClientBuilder s3ClientBuilder = S3Client.builder()
          .credentialsProvider(awsCredentialsProvider)
          .endpointOverride(URI.create(endpoint))
          .forcePathStyle(forcePathStyle
              /* true: use endpoint.tld/bucket
                 false use bucket.endpoint.tld  */
          )
          .serviceConfiguration(builder ->
                  builder.chunkedEncodingEnabled(false)
                /* disable chunked encoding preventing the following error:
                   "Transfering payloads in multiple chunks using aws-chunked is not supported." */
          );

      if (StringUtils.isNotBlank(region)) {
        final Region regionObj = Region.of(region);
        log.debug("set region[{}]: {}", region, regionObj);
        s3ClientBuilder.region(regionObj);
      }

      s3 = s3ClientBuilder.build();
      log.debug("Created S3 bucket {}.", s3);
    }
    return s3;
  }

  private void resetS3ClientToEnforceRefresh() {
    try {
      if (s3 != null) {
        s3.close();
        log.debug("Closed S3Client.");
      }
    } catch (Exception e) {
      log.warn("Failed to close S3Client: {}.", e.getMessage());
    }
    s3 = null;
  }
}
