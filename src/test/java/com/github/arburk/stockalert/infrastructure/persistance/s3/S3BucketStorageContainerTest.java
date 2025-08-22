package com.github.arburk.stockalert.infrastructure.persistance.s3;

import com.github.arburk.stockalert.application.config.JacksonConfig;
import com.github.arburk.stockalert.application.domain.Security;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MinIOContainer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.github.arburk.stockalert.application.service.stock.PersistanceProvider.STORAGE_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S3BucketStorageContainerTest {

  private static final MinIOContainer MINIO_TESTCONTAINER = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
      .withUserName("testuser")
      .withPassword("testpassword");
  private static final String BUCKET_NAME = "stock-alert";
  private static final Collection<Security> SECURITIES = Arrays.asList(
      new Security("ABC", 19.1, "CHF", LocalDateTime.now(), "SIX"),
      new Security("DEF", 755.9, "EUR", LocalDateTime.now(), "XETRA"),
      new Security("GHI", 0.47, "USD", LocalDateTime.now(), "NASDAQ"),
      new Security("JKL", 82.24, "GBP", LocalDateTime.now(), "LSX")
  );

  private S3BucketStorage testee;
  private S3Client s3Client;

  @BeforeAll
  static void beforeAll() {
    MINIO_TESTCONTAINER.start();
  }

  @AfterAll
  static void afterAll() {
    MINIO_TESTCONTAINER.stop();
  }

  @BeforeEach
  void setUp() {
    testee = new S3BucketStorage(new JacksonConfig().objectMapper());
    ReflectionTestUtils.setField(testee, "endpoint", MINIO_TESTCONTAINER.getS3URL());
    ReflectionTestUtils.setField(testee, "accessKey", MINIO_TESTCONTAINER.getUserName());
    ReflectionTestUtils.setField(testee, "secretKey", MINIO_TESTCONTAINER.getPassword());
    ReflectionTestUtils.setField(testee, "region", "test-region");
    ReflectionTestUtils.setField(testee, "forcePathStyle", true);
    ReflectionTestUtils.setField(testee, "bucket", BUCKET_NAME);
    s3Client = ReflectionTestUtils.invokeMethod(testee, "getS3");
  }

  @Test
  @Order(1)
  void writeDataIntoBucket() {
    ReflectionTestUtils.setField(testee, "bucket", BUCKET_NAME);
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

    assertDoesNotThrow(() -> testee.updateSecurities(SECURITIES));

    final List<S3Object> s3Contents = s3Client.listObjects(ListObjectsRequest.builder()
            .bucket(BUCKET_NAME).prefix(STORAGE_FILE_NAME).build())
        .contents()
        .stream()
        .toList();
    assertNotNull(s3Contents);
    assertFalse(s3Contents.isEmpty());
    assertEquals(1, s3Contents.size());
  }

  @Test
  @Order(2)
  void readDataFromBucket() {
    assertDoesNotThrow(() -> {
      final Collection<Security> securites = ReflectionTestUtils.invokeMethod(testee, "initData");
      assertEquals(SECURITIES, securites);
    });
  }

}