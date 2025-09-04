package com.github.arburk.stockalert.infrastructure.persistance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.config.JacksonConfig;
import com.github.arburk.stockalert.application.domain.MetaInfo;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.StockAlertDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageTest {

  private static final Path TEST_HOME = Paths.get("target/test/", String.valueOf(System.currentTimeMillis()));
  private static final Path EXPECTED_FILE_PATH = Path.of(TEST_HOME.toString(), "stock-alert", "securities.db.json");
  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2018, Month.SEPTEMBER, 24, 17, 12, 1, 12);
  private static final LocalDateTime EXPECTED_LAST_ERROR = LocalDateTime.of(2025, Month.AUGUST, 24, 18, 17, 35, 12);

  private String userHome;
  private FileStorage testee;

  @BeforeEach
  void setUp() {
    userHome = System.getProperty("user.home");
    System.setProperty("user.home", TEST_HOME.toString());
    testee = new FileStorage(new JacksonConfig().objectMapper());
  }

  @AfterEach
  void tearDown() {
    if (EXPECTED_FILE_PATH.toFile().exists()) {
      assertTrue(EXPECTED_FILE_PATH.toFile().getAbsoluteFile().delete(),
          "Failed to cleanup test store %s".formatted(EXPECTED_FILE_PATH.toFile().getAbsoluteFile()));
    }
    System.setProperty("user.home", userHome);
  }

  @Test
  void test_ReadData_inexistenBaseFile_NothingBreaking() {
    assertTrue(testee.getSecurites().isEmpty());
  }

  @Test
  void test_ReadData_FileExists() throws IOException {
    final File parentDir = EXPECTED_FILE_PATH.toFile().getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs();
    }
    try (FileWriter fileWriter = new FileWriter(EXPECTED_FILE_PATH.toFile().getAbsoluteFile())) {
      fileWriter.write(Files.readString(Path.of("src/test/resources/storage/expected-initial-result.json")));
    }
    final Collection<Security> securites = testee.getSecurites();

    assertAll(
        () -> assertFalse(securites.isEmpty()),
        () -> assertEquals(3, securites.size()),
        () -> assertSecurity(securites, "BALN", 199.99),
        () -> assertSecurity(securites, "MOZN", 11.24),
        () -> assertSecurity(securites, "ROG", 251.34)
    );
  }

  @Test
  void test_ReadMetaInfo_FileExists() throws IOException {
    final File parentDir = EXPECTED_FILE_PATH.toFile().getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs();
    }
    try (FileWriter fileWriter = new FileWriter(EXPECTED_FILE_PATH.toFile().getAbsoluteFile())) {
      fileWriter.write(Files.readString(Path.of("src/test/resources/storage/expected-initial-result.json")));
    }
    final MetaInfo metaInfo = testee.getMetaInfo();

    assertAll(
        () -> assertNotNull(metaInfo),
        () -> assertNotNull(metaInfo.lastErrorAlert()),
        () -> assertEquals(EXPECTED_LAST_ERROR, metaInfo.lastErrorAlert())
    );
  }

  @Test
  void test_InitData_ExcpetionReturnsEmptyDataFile() throws IOException {
    final Path pathMock = mock(Path.class);
    final ObjectMapper objectMapperMock = mock(ObjectMapper.class);
    ReflectionTestUtils.setField(testee, "filePath", pathMock);
    ReflectionTestUtils.setField(testee, "objectMapper", objectMapperMock);
    doThrow(new IOException("Test Exception")).when(objectMapperMock).readValue(any(File.class), any(Class.class));
    final File mockedFile = mock(File.class);
    when(pathMock.toFile()).thenReturn(mockedFile);
    when(mockedFile.exists()).thenReturn(true);

    final StockAlertDb stockAlertDb = assertDoesNotThrow(testee::initData);
    assertNotNull(stockAlertDb);
    assertNotNull(stockAlertDb.securities());
    assertTrue(stockAlertDb.securities().isEmpty());
    assertNull(stockAlertDb.metaInfo());
  }

  private static void assertSecurity(final Collection<Security> securites, final String symbolName, final double expected) {
    final Security stock = securites.stream().filter(sec -> symbolName.equals(sec.symbol())).findFirst().get();
    assertEquals(expected, stock.price());
    assertEquals("CHF", stock.currency());
    assertEquals("Switzerland", stock.exchange());
    assertEquals(TIMESTAMP, stock.timestamp());
  }

  @Test
  void test_WriteInitialData_HappyFlow() throws IOException {
    final List<Security> securities = List.of(
        new Security("BALN", 199.99, "CHF", null, TIMESTAMP, "Switzerland", null),
        new Security("MOZN", 11.24, "CHF", .0121, TIMESTAMP, "Switzerland", null),
        new Security("ROG", 251.34, "CHF", -.0324, TIMESTAMP, "Switzerland", null)
    );

    testee.updateSecurities(securities);
    testee.updateMetaInfo(new MetaInfo(EXPECTED_LAST_ERROR));

    assertTrue(EXPECTED_FILE_PATH.toFile().exists());

    final String generatedContents = Files.readString(EXPECTED_FILE_PATH);
    final String expectedResult = Files.readString(Path.of("src/test/resources/storage/expected-initial-result.json"));
    final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    assertEquals(objectMapper.readTree(expectedResult), objectMapper.readTree(generatedContents),
        "JSON-contents do not match");
  }

  @Test
  void updateData() throws IOException {
    final List<Security> initalData = List.of(
        new Security("BALN", 199.99, "CHF", null, TIMESTAMP, "Switzerland", null),
        new Security("MOZN", 11.24, "CHF", null, TIMESTAMP, "Switzerland", null),
        new Security("ROG", 251.34, "CHF", null, TIMESTAMP, "Switzerland", null)
    );

    testee.updateSecurities(initalData); //inital data is written

    final LocalDateTime newTs = LocalDateTime.of(2025, Month.AUGUST, 6, 14, 39, 12, 11);
    final List<Security> updatedData = List.of(
        new Security("BALN", 212.14, "CHF", .0217, newTs, "Switzerland", null),
        new Security("MOZN", 9.36, "CHF", -.0026, newTs, "unknown", null),
        new Security("NOVN", 96.24, "CHF", null, newTs, "Switzerland", null)
    );

    testee.updateSecurities(updatedData); //update stuff

    assertTrue(EXPECTED_FILE_PATH.toFile().exists());
    final String generatedContents = Files.readString(EXPECTED_FILE_PATH);
    final String expectedContents = Files.readString(Path.of("src/test/resources/storage/expected-update-result_sorted.json"));
    final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    assertEquals(objectMapper.readTree(expectedContents), objectMapper.readTree(generatedContents),
        "JSON-contents do not match");
  }

}