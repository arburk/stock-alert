package com.github.arburk.stockalert.infrastructure.persistance.s3;

import com.github.arburk.stockalert.application.config.JacksonConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.service.stock.PersistanceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class S3BucketStorageTest {

  private static final String TEST_BUCKET = "test-bucket";
  private S3BucketStorage testee;
  private S3Client mockS3;

  @BeforeEach
  void setUp() {
    mockS3 = mock(S3Client.class);

    testee = new S3BucketStorage(new JacksonConfig().objectMapper());
    ReflectionTestUtils.setField(testee, "endpoint", "http://s3.bucket.com:9000");
    ReflectionTestUtils.setField(testee, "forcePathStyle", true);
    ReflectionTestUtils.setField(testee, "accessKey", "myKey");
    ReflectionTestUtils.setField(testee, "secretKey", "mySecret");
    ReflectionTestUtils.setField(testee, "region", "us-east-1");
    ReflectionTestUtils.setField(testee, "bucket", TEST_BUCKET);
    ReflectionTestUtils.setField(testee, "s3", mockS3);
  }

  @Test
  void initData_noFileFound_returnsEmptyList() {
    final ArgumentCaptor<ListObjectsRequest> requestCaptor = ArgumentCaptor.forClass(ListObjectsRequest.class);
    when(mockS3.listObjects(requestCaptor.capture()))
        .thenReturn(ListObjectsResponse.builder().contents(List.of()).build());

    var result = testee.getSecurites();

    assertTrue(result.isEmpty());
    final List<ListObjectsRequest> allRequests = requestCaptor.getAllValues();
    assertEquals(2, allRequests.size());
    final ListObjectsRequest firstRequest = allRequests.getFirst();
    final ListObjectsRequest secondRequest = allRequests.getLast();
    verify(mockS3).listObjects(firstRequest);
    verify(mockS3).listObjects(secondRequest);
    assertEquals(TEST_BUCKET, firstRequest.bucket());
    assertEquals(TEST_BUCKET, secondRequest.bucket());
    assertEquals(PersistanceProvider.STORAGE_FILE_NAME, firstRequest.prefix());
    assertEquals(PersistanceProvider.STORAGE_FILE_NAME_0_1_3, secondRequest.prefix());
    verify(mockS3, never()).getObject(any(GetObjectRequest.class));
  }

  @Test
  void initData_ExceptionThrown_returnsEmptyList() {
    when(mockS3.listObjects(any(ListObjectsRequest.class)))
        .thenThrow(NoSuchBucketException.builder().message("Test Message").build());

    var result = testee.getSecurites();

    assertTrue(result.isEmpty());
    verify(mockS3).close();
    assertNull(ReflectionTestUtils.getField(testee, "s3"));
    verify(mockS3, never()).getObject(any(GetObjectRequest.class));
  }

  @Test
  void initData_oneFileFound_readsData() throws Exception {
    final ArgumentCaptor<ListObjectsRequest> listRequestCaptor = ArgumentCaptor.forClass(ListObjectsRequest.class);
    final S3Object storageFile = S3Object.builder().key(PersistanceProvider.STORAGE_FILE_NAME).build();
    when(mockS3.listObjects(listRequestCaptor.capture()))
        .thenReturn(ListObjectsResponse.builder().contents(List.of(storageFile)).build());

    final LocalDateTime timestamp = LocalDateTime.now();
    final Security sec = new Security("AAPL", 154.2, "USD", timestamp, "NYSE");
    final byte[] json = new JacksonConfig().objectMapper().writeValueAsBytes(List.of(sec));
    final ArgumentCaptor<GetObjectRequest> getRequestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);

    when(mockS3.getObject(getRequestCaptor.capture()))
        .thenReturn(new ResponseInputStream<>(GetObjectResponse.builder().build(), new ByteArrayInputStream(json)));

    var result = testee.getSecurites();

    assertEquals(1, result.size());
    final Security first = result.stream().toList().getFirst();
    assertEquals("AAPL", first.symbol());
    assertEquals(154.2, first.price());
    assertEquals("USD", first.currency());
    assertEquals("NYSE", first.exchange());
    assertEquals(timestamp, first.timestamp());

    final ListObjectsRequest capturedListReq = listRequestCaptor.getValue();
    verify(mockS3).listObjects(capturedListReq);
    assertEquals(TEST_BUCKET, capturedListReq.bucket());
    assertEquals(PersistanceProvider.STORAGE_FILE_NAME, capturedListReq.prefix());

    final GetObjectRequest capturedGetReq = getRequestCaptor.getValue();
    verify(mockS3).getObject(capturedGetReq);
    assertEquals(TEST_BUCKET, capturedGetReq.bucket());
    assertEquals(PersistanceProvider.STORAGE_FILE_NAME, capturedGetReq.key());
    verify(mockS3, never()).close();
  }

  @Test
  void updateSecurities_emptyList_skipsPersist() {
    testee.updateSecurities(List.of());
    verify(mockS3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    verify(mockS3, never()).close();
  }

  @Test
  void updateSecurities_withData_persistsSuccessfully() throws IOException {
    final LocalDateTime timestamp = LocalDateTime.now();
    final Security secOld = new Security("AAPL", 143.2, "USD", timestamp, "NYSE");
    final Security secNew = new Security("AAPL", 19.2, "USD", timestamp, "NYSE");
    final ArrayList<Object> objects = new ArrayList<>();
    objects.add(secOld);
    ReflectionTestUtils.setField(testee, "data", objects);
    ArgumentCaptor<PutObjectRequest> putObjReqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
    when(mockS3.putObject(putObjReqCaptor.capture(), requestBodyCaptor.capture()))
        .thenReturn(PutObjectResponse.builder().build());

    assertEquals(secOld, testee.getSecurites().stream().toList().getFirst());

    testee.updateSecurities(List.of(secNew));

    final Collection<Security> newData = testee.getSecurites();
    assertEquals(secNew, newData.stream().toList().getFirst());
    final PutObjectRequest putObjValueCaptured = putObjReqCaptor.getValue();
    final RequestBody requestBodyCaptured = requestBodyCaptor.getValue();
    verify(mockS3).putObject(putObjValueCaptured, requestBodyCaptured);
    assertEquals(TEST_BUCKET, putObjValueCaptured.bucket());
    assertEquals(PersistanceProvider.STORAGE_FILE_NAME, putObjValueCaptured.key());
    assertTrue(requestBodyCaptured.optionalContentLength().isPresent());
    final StringWriter jsonWriter = new StringWriter();
    new JacksonConfig().objectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonWriter, newData);
    final int expectedContentLength = jsonWriter.toString().getBytes(StandardCharsets.UTF_8).length;
    assertEquals(expectedContentLength, requestBodyCaptured.optionalContentLength().get());
    verify(mockS3, never()).close();
  }

  @Test
  void persistInFile_whenPutFails_resetsClient() {
    final ArrayList<Object> objects = new ArrayList<>();
    objects.add(new Security("MSFT", 143.2, "USD", LocalDateTime.now(), "NYSE"));
    ReflectionTestUtils.setField(testee, "data", objects);
    when(mockS3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(S3Exception.builder().message("Could not upload").build());

    final Security sec = new Security("AAPL", 143.2, "USD", LocalDateTime.now(), "NYSE");
    assertDoesNotThrow(() -> testee.updateSecurities(List.of(sec)));

    verify(mockS3).close();
    assertNull(ReflectionTestUtils.getField(testee, "s3")); // client should be reset to null
  }

  @Test
  void shutdown_closesClient() {
    testee.shutdown();
    verify(mockS3).close();
  }

  @Test
  void getS3Client_Parameter() {
    ReflectionTestUtils.setField(testee, "s3", null /* reset mock first*/);

    try (MockedStatic<S3Client> mockedStaticS3Client = mockStatic(S3Client.class)) {

      final S3ClientBuilder mockedClientBuilder = mock(S3ClientBuilder.class);
      mockedStaticS3Client.when(S3Client::builder).thenReturn(mockedClientBuilder);

      when(mockedClientBuilder.endpointOverride(any(URI.class))).thenReturn(mockedClientBuilder);
      when(mockedClientBuilder.forcePathStyle(anyBoolean())).thenReturn(mockedClientBuilder);
      when(mockedClientBuilder.region(any(Region.class))).thenReturn(mockedClientBuilder);
      when(mockedClientBuilder.serviceConfiguration(any(Consumer.class))).thenReturn(mockedClientBuilder);
      final var credentialsProviderCaptor = ArgumentCaptor.forClass(StaticCredentialsProvider.class);
      when(mockedClientBuilder.credentialsProvider(credentialsProviderCaptor.capture())).thenReturn(mockedClientBuilder);

      //act
      assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(testee, "getS3"));

      verify(mockedClientBuilder).endpointOverride(URI.create("http://s3.bucket.com:9000"));
      verify(mockedClientBuilder).forcePathStyle(true);
      verify(mockedClientBuilder).region(Region.of("us-east-1"));

      final StaticCredentialsProvider value = credentialsProviderCaptor.getValue();
      assertNotNull(value);
      assertEquals("myKey", value.resolveCredentials().accessKeyId());
      assertEquals("mySecret", value.resolveCredentials().secretAccessKey());
    }


  }
}