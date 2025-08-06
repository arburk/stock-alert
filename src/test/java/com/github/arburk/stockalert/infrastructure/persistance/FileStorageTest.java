package com.github.arburk.stockalert.infrastructure.persistance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.config.JacksonConfig;
import com.github.arburk.stockalert.application.domain.Security;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageTest {

  private String userHome;
  private static final Path TEST_HOME = Paths.get("target/test/", String.valueOf(System.currentTimeMillis()));
  private static final Path EXPECTED_FILE_PATH = Path.of(TEST_HOME.toString(), "stock-alert", "securities.db");
  private static final LocalDateTime timestamp = LocalDateTime.of(2018, Month.SEPTEMBER, 24, 17, 12, 1, 12);

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

  private static void assertSecurity(final Collection<Security> securites, final String symbolName, final double expected) {
    final Security stock = securites.stream().filter(sec -> symbolName.equals(sec.symbol())).findFirst().get();
    assertEquals(expected, stock.price());
    assertEquals("CHF", stock.currency());
    assertEquals("Switzerland", stock.exchange());
    assertEquals(timestamp, stock.timestamp());
  }

  @Test
  void test_WriteInitialData_HappyFlow() throws IOException {
    final List<Security> securities = List.of(
        new Security("BALN", 199.99, "CHF", timestamp, "Switzerland"),
        new Security("MOZN", 11.24, "CHF", timestamp, "Switzerland"),
        new Security("ROG", 251.34, "CHF", timestamp, "Switzerland")
    );

    testee.updateSecurities(securities);

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
        new Security("BALN", 199.99, "CHF", timestamp, "Switzerland"),
        new Security("MOZN", 11.24, "CHF", timestamp, "Switzerland"),
        new Security("ROG", 251.34, "CHF", timestamp, "Switzerland")
    );

    testee.updateSecurities(initalData); //inital data is written

    final LocalDateTime newTs = LocalDateTime.of(2025, Month.AUGUST, 6, 14, 39, 12, 11);
    final List<Security> updatedData = List.of(
        new Security("BALN", 212.14, "CHF", newTs, "Switzerland"),
        new Security("MOZN", 9.36, "CHF", newTs, "unknown"),
        new Security("NOVN", 96.24, "CHF", newTs, "Switzerland")
    );

    testee.updateSecurities(updatedData); //update stuff

    assertTrue(EXPECTED_FILE_PATH.toFile().exists());
    final String generatedContents = Files.readString(EXPECTED_FILE_PATH);
    final String expectedContents = Files.readString(Path.of("src/test/resources/storage/expected-update-result.json"));
    final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    assertEquals(objectMapper.readTree(expectedContents), objectMapper.readTree(generatedContents),
        "JSON-contents do not match");
  }

}