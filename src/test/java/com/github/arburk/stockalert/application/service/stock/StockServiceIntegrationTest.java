package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.domain.Alert;
import com.github.arburk.stockalert.application.domain.Security;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.wiremock.spring.EnableWireMock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock
@ActiveProfiles({"stock-service-int-test"})
@TestPropertySource(properties = {
    "stock-alert.base-url=${wiremock.server.baseUrl}",
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StockServiceIntegrationTest {

  private static final String USER_HOME = System.getProperty("user.home");
  private static final Path TEST_HOME = Path.of("target/test/", String.valueOf(System.currentTimeMillis()));
  private static File expectedStorageFile;
  private static GreenMail greenMail;

  @Autowired
  private StockService stockService;

  @Autowired
  private PersistenceProvider persistenceProvider;

  @BeforeAll
  static void setUpAll() {
    System.setProperty("user.home", TEST_HOME.toString() /* redirect file storage to test */);
    expectedStorageFile = Path.of(TEST_HOME.toString(), "stock-alert", PersistenceProvider.STORAGE_FILE_NAME).toFile();
    greenMail = new GreenMail(new ServerSetup(0 /* 0 = random port */, null, ServerSetup.PROTOCOL_SMTP));
    greenMail.setUser("test-user", "test-pass" /* defined in src/test/resources/application-stock-service-int-test.yml */);
    greenMail.start();
    System.setProperty("spring.mail.port", String.valueOf(greenMail.getSmtp().getPort()));
  }

  @AfterAll
  static void tearDownAll() {
    System.setProperty("user.home", USER_HOME /* rest user.home after test to default */);
  }

  @BeforeEach
  void setUp() {
    assertTrue(greenMail.isRunning());
  }

  @AfterEach
  void tearDown() {
    if (expectedStorageFile.exists()) {
      assertTrue(expectedStorageFile.delete());
    }
  }

  @Test
  @Order(1)
    // no mail sent yet
  void apiNotReachable() {
    stubFor(get(urlPathMatching("/v8/finance/chart/.*")).willReturn(serverError()));

    // now execute
    assertDoesNotThrow(() -> stockService.update(), "error in API communication should not break the service");

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
    //TODO: change when error notification is implemented
    assertEquals(0, receivedMessages.length, "Mail message was not expected to be sent");
  }

  @Test
  @Order(2)
  void happyFlow_WithAlerting() throws MessagingException, IOException {
    stubAllChartSymbols();

    assertFalse(expectedStorageFile.exists());
    getPreparedSecurities().forEach(persistenceProvider::updateSecurity);
    persistenceProvider.commitChanges(/* peristed file required for comparison */);
    assertTrue(expectedStorageFile.exists(), "Expected FileStorage created file %s, but nothing found".formatted(expectedStorageFile.toString()));
    final var fileSizeBeforeAddingResults = expectedStorageFile.length();
    assertTrue(persistenceProvider.getSecurites().stream().allMatch(sec -> sec.alertLog().isEmpty()));

    // now execute
    stockService.update();

    final Collection<Security> securites = persistenceProvider.getSecurites();
    assertEquals(5, securites.size(), "expected all stubbed symbols persisted; NOSTUB.SW is skipped");
    final Security baln = securites.stream().filter(security -> "BALN.SW".equals(security.symbol())).findFirst()
        .orElseThrow(() -> new RuntimeException("Test failed. Expected Security is present."));
    assertEquals(207.4, baln.price());
    assertTrue(expectedStorageFile.length() > fileSizeBeforeAddingResults, "expected more contents in storage file, but size did not grow");

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
    assertEquals(1, receivedMessages.length, "Mail message was expected to be sent, which was apparently not the case");
    final MimeMessage receivedMessage = receivedMessages[0];
    assertEquals("Threshold CHF 200.0 for BALN.SW crossed", receivedMessage.getSubject());
    final List<String> expectedMailBodyLines = List.of(
        // latest timestamp comes from epoch seconds converted to the system default zone -> match via regex
        "Price for BALN.SW raised to CHF 207.4 dated on \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2} - from formerly CHF 198.15 dated on 2025-08-07 15:06",
        "Test comment to be sent | alert comment to be present",
        "Data refers to stock exchange Switzerland."
    );
    assertLinesMatch(expectedMailBodyLines, Arrays.asList(receivedMessage.getContent().toString().split("\\R")));

    final Collection<Alert> alerts = persistenceProvider.getSecurity(baln)
        .orElseThrow(() -> new RuntimeException("Test failed. Expected Security is present.")).alertLog();
    assertNotNull(alerts);
    assertEquals(1, alerts.size());
    final Alert first = alerts.stream().toList().getFirst();
    assertEquals(200, first.threshold());
    assertEquals("CHF", first.unit());
  }

  @Test
  @Order(3)
  void happyFlow_WithPercentageAlerting() throws MessagingException, IOException {
    stubChart("INGA.AS", "chart-INGA.AS-percentage.json" /* all other symbols remain unstubbed and are skipped */);

    assertFalse(expectedStorageFile.exists());
    ReflectionTestUtils.setField(persistenceProvider, "data", null); //
    getPreparedSecurities().forEach(persistenceProvider::updateSecurity);
    persistenceProvider.commitChanges(/* peristed file required for comparison */);
    assertTrue(expectedStorageFile.exists(), "Expected FileStorage created file %s, but nothing found".formatted(expectedStorageFile.toString()));
    assertTrue(persistenceProvider.getSecurites().stream().allMatch(sec -> sec.alertLog().isEmpty()));

    // now execute
    stockService.update();

    assertEquals(2, persistenceProvider.getSecurites().size());
    final Security inga = persistenceProvider.getSecurity(new Security("INGA.AS", null, null, null, null, "Amsterdam", null))
        .orElseThrow(() -> new RuntimeException("Test failed. Expected Security is present."));
    assertEquals(20.18 /* > 5% deviation compared to getPreparedSecurities()*/, inga.price());

    final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
    assertTrue(receivedMessages.length > 0, "percentage alert email was expected to be sent, which was apparently not the case");
    final MimeMessage receivedMessage = Arrays.asList(receivedMessages).getLast();
    assertEquals("Threshold of 5.00 % crossed for INGA.AS", receivedMessage.getSubject());
    final List<String> expectedMailBodyLines = List.of(
        "Price for INGA.AS raised to EUR 20.18 - from formerly EUR 18.95 dated on 2025-08-07 15:06.",
        "Price change is 6.49 % while defined threshold is 5.00 %.",
        // latest timestamp comes from epoch seconds converted to the system default zone -> match via regex
        "Data refers to stock exchange Amsterdam dated on \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}."
    );
    assertLinesMatch(expectedMailBodyLines, Arrays.asList(receivedMessage.getContent().toString().split("\\R")));

    final Collection<Alert> alerts = persistenceProvider.getSecurity(inga)
        .orElseThrow(() -> new RuntimeException("Test failed. Expected Security is present.")).alertLog();
    assertNotNull(alerts);
    assertEquals(1, alerts.size());
    final Alert first = alerts.stream().toList().getFirst();
    assertEquals(0.06490765171503954, first.threshold());
    assertEquals("%", first.unit());
  }

  @Test
  @Order(999)
  void emailNotSendable() {
    stubAllChartSymbols();

    if (expectedStorageFile.exists()) {
      assertTrue(expectedStorageFile.delete());
    }
    getPreparedSecurities().forEach(persistenceProvider::updateSecurity);
    persistenceProvider.commitChanges(/* peristed file required for comparison */);

    greenMail.stop();

    // now execute
    assertDoesNotThrow(() -> stockService.update(), "error in notification service");

    final Collection<Security> securites = persistenceProvider.getSecurites();
    assertEquals(5, securites.size());
    final Security baln = securites.stream().filter(security -> "BALN.SW".equals(security.symbol())).findFirst()
        .orElseThrow(() -> new RuntimeException("Test failed. Expected Security is present."));
    assertEquals(207.4, baln.price());

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
    assertEquals(0, receivedMessages.length, "No Mail was expected to be sent, but found one");
  }

  private static void stubAllChartSymbols() {
    // NOSTUB.SW from integration-test-config.json is deliberately left unstubbed (WireMock answers 404)
    // to verify that a failing symbol does not break the batch
    stubChart("BALN.SW", "chart-BALN.SW.json");
    stubChart("INGA.AS", "chart-INGA.AS.json");
    stubChart("NESN.SW", "chart-NESN.SW.json");
    stubChart("ROG.SW", "chart-ROG.SW.json");
    stubChart("MMM", "chart-MMM.json");
  }

  private static void stubChart(final String symbol, final String fixture) {
    stubFor(get(urlPathEqualTo("/v8/finance/chart/" + symbol))
        .withQueryParam("interval", equalTo("1d"))
        .withQueryParam("range", equalTo("1d"))
        .willReturn(okJson(Files.contentOf(Path.of("src/test/resources/rest-client/yahoo/" + fixture).toFile(), StandardCharsets.UTF_8))));
  }

  private Collection<Security> getPreparedSecurities() {
    // For BALN.SW a threshold of 200 is defined in src/test/resources/config/integration-test-config.json
    // so we set a value lower (198.15) here and a value higher (207.4) in src/test/resources/rest-client/yahoo/chart-BALN.SW.json
    // to trigger alert
    final LocalDateTime timestamp = LocalDateTime.of(2025, Month.AUGUST, 7, 15, 6, 1, 99);
    return List.of(
        new Security("BALN.SW", 198.15, "CHF", null, timestamp, "Switzerland", null),
        new Security("INGA.AS", 18.95, "EUR", null, timestamp, "Amsterdam", null)
    );
  }

}
