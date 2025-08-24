package com.github.arburk.stockalert.application.service.stock;

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
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
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
  private static final Path TEST_HOME = Paths.get("target/test/", String.valueOf(System.currentTimeMillis()));
  private static File expectedStorageFile;
  private static GreenMail greenMail;

  @Autowired
  private StockService stockService;

  @Autowired
  private PersistenceProvider persistenceProvider;

  @BeforeAll
  static void setUpAll() {
    System.setProperty("user.home", TEST_HOME.toString()); // redirect file storage to test
    expectedStorageFile = Path.of(TEST_HOME.toString(), "stock-alert", PersistenceProvider.STORAGE_FILE_NAME).toFile();
    greenMail = new GreenMail(new ServerSetup(0 /* 0 = random port */, null, ServerSetup.PROTOCOL_SMTP));
    greenMail.setUser("test-user", "test-pass"); // defined in src/test/resources/application-stock-service-int-test.yml
    greenMail.start();
    System.setProperty("spring.mail.port", String.valueOf(greenMail.getSmtp().getPort()));
  }

  @AfterAll
  static void tearDownAll() {
    System.setProperty("user.home", USER_HOME);  // rest user.home after test to default
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
    stubFor(get("/latest?symbol=ENI%2CFRO%2CPPGN%2CSUNN%2CMBLY%2CMRK%2CSIKA%2CALV%2CBSLN%2CPFE%2CSRAIL%2CVODI%2CLEHN%2CINGA%2CHELN%2CBALN%2CSTMN%2CVACD%2CCMBN%2CAMRZ%2CROG%2CMBTN%2CMMM%2CVUL%2CBANB%2CBIOV%2CBAER%2CMOZN%2CDTE%2CLEON%2CDKSH%2CDVN&access_key=API_KEY")
        .willReturn(serverError()));

    // now execute
    assertDoesNotThrow(() -> stockService.update(), "error in API communication should not break the service");

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
    //TODO: change when error notification is implemented
    assertEquals(0, receivedMessages.length, "Mail message was not expected to be sent");
  }

  @Test
  @Order(2)
  void happyFlow_WithAlerting() throws MessagingException, IOException {
    stubFor(get("/latest?symbol=ENI%2CFRO%2CPPGN%2CSUNN%2CMBLY%2CMRK%2CSIKA%2CALV%2CBSLN%2CPFE%2CSRAIL%2CVODI%2CLEHN%2CINGA%2CHELN%2CBALN%2CSTMN%2CVACD%2CCMBN%2CAMRZ%2CROG%2CMBTN%2CMMM%2CVUL%2CBANB%2CBIOV%2CBAER%2CMOZN%2CDTE%2CLEON%2CDKSH%2CDVN&access_key=API_KEY")
        .willReturn(okJson(Files.contentOf(Path.of("src/test/resources/rest-client/extended-response.json").toFile(), StandardCharsets.UTF_8))));

    assertFalse(expectedStorageFile.exists());
    persistenceProvider.updateSecurities(/* peristed file required for comparison */ getPreparedSecurities());
    assertTrue(expectedStorageFile.exists(), "Expected FileStorage created file %s, but nothing found".formatted(expectedStorageFile.toString()));
    final var fileSizeBeforeAddingResults = expectedStorageFile.length();

    // now execute
    stockService.update();

    final Collection<Security> securites = persistenceProvider.getSecurites();
    assertEquals(32, securites.size());
    final Security baln = securites.stream().filter(security -> "BALN".equals(security.symbol())).findFirst().get();
    assertEquals(207.4, baln.price());
    assertTrue(expectedStorageFile.length() > fileSizeBeforeAddingResults, "expected more contents in storage file, but size did not grow");

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
    assertEquals(1, receivedMessages.length, "Mail message was expected to be sent, which was apparently not the case");
    final MimeMessage receivedMessage = receivedMessages[0];
    assertEquals("Threshold CHF 200.0 for BALN crossed", receivedMessage.getSubject());
    final List<String> expectedMailBodyLines = List.of(
        "Price for BALN moved from CHF 198.15 dated on 2025-08-07 15:06 to CHF 207.4 dated on 2025-08-07 08:35",
        "Data refers to stock exchange Switzerland."
    );
    assertLinesMatch(expectedMailBodyLines, Arrays.asList(receivedMessage.getContent().toString().split("\\R")));
  }

  @Test
  @Order(3)
  void happyFlow_WithPercentageAlerting() throws MessagingException, IOException {
    stubFor(get("/latest?symbol=ENI%2CFRO%2CPPGN%2CSUNN%2CMBLY%2CMRK%2CSIKA%2CALV%2CBSLN%2CPFE%2CSRAIL%2CVODI%2CLEHN%2CINGA%2CHELN%2CBALN%2CSTMN%2CVACD%2CCMBN%2CAMRZ%2CROG%2CMBTN%2CMMM%2CVUL%2CBANB%2CBIOV%2CBAER%2CMOZN%2CDTE%2CLEON%2CDKSH%2CDVN&access_key=API_KEY")
        .willReturn(okJson(Files.contentOf(Path.of("src/test/resources/rest-client/response_percentage_test.json").toFile(), StandardCharsets.UTF_8))));

    assertFalse(expectedStorageFile.exists());
    ReflectionTestUtils.setField(persistenceProvider, "data", null); //
    persistenceProvider.updateSecurities(/* peristed file required for comparison */ getPreparedSecurities());
    assertTrue(expectedStorageFile.exists(), "Expected FileStorage created file %s, but nothing found".formatted(expectedStorageFile.toString()));

    // now execute
    stockService.update();

    final Collection<Security> securites = persistenceProvider.getSecurites();
    assertEquals(2, securites.size());
    final Security inga = securites.stream().filter(security -> "INGA".equals(security.symbol())).findFirst().get();
    assertEquals(20.18 /* > 5% deviation compared to getPreparedSecurities()*/, inga.price());

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

    assertTrue(receivedMessages.length > 0, "percentage alert email was expected to be sent, which was apparently not the case");
    final MimeMessage receivedMessage = Arrays.asList(receivedMessages).getLast();
    assertEquals("Threshold of 5.00 % crossed for INGA", receivedMessage.getSubject());
    final List<String> expectedMailBodyLines = List.of(
        "Price for INGA moved from EUR 18.95 dated on 2025-08-07 15:06 to EUR 20.18.",
        "Price change is 6.49 % while defined threshold is 5.00 %.",
        "Data refers to stock exchange Amsterdam dated on 2025-08-07 08:52."
    );
    assertLinesMatch(expectedMailBodyLines, Arrays.asList(receivedMessage.getContent().toString().split("\\R")));
  }

  @Test
  @Order(999)
  void emailNotSendable() {
    stubFor(get("/latest?symbol=ENI%2CFRO%2CPPGN%2CSUNN%2CMBLY%2CMRK%2CSIKA%2CALV%2CBSLN%2CPFE%2CSRAIL%2CVODI%2CLEHN%2CINGA%2CHELN%2CBALN%2CSTMN%2CVACD%2CCMBN%2CAMRZ%2CROG%2CMBTN%2CMMM%2CVUL%2CBANB%2CBIOV%2CBAER%2CMOZN%2CDTE%2CLEON%2CDKSH%2CDVN&access_key=API_KEY")
        .willReturn(okJson(Files.contentOf(Path.of("src/test/resources/rest-client/extended-response.json").toFile(), StandardCharsets.UTF_8))));

    if (expectedStorageFile.exists()) {
      assertTrue(expectedStorageFile.delete());
    }
    persistenceProvider.updateSecurities(/* peristed file required for comparison */ getPreparedSecurities());

    greenMail.stop();

    // now execute
    assertDoesNotThrow(() -> stockService.update(), "error in notification service");

    final Collection<Security> securites = persistenceProvider.getSecurites();
    assertEquals(32, securites.size());
    final Security baln = securites.stream().filter(security -> "BALN".equals(security.symbol())).findFirst().get();
    assertEquals(207.4, baln.price());

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
    assertEquals(0, receivedMessages.length, "No Mail was expected to be sent, but found one");
  }

  private Collection<Security> getPreparedSecurities() {
    // For BALN a threshold of 200 is defined in src/test/resources/config/integration-test-config.json
    // so we set a value lower (198.15) here and a value higher (207.4) in src/test/resources/rest-client/extended-response.json
    // to trigger alert
    final LocalDateTime timestamp = LocalDateTime.of(2025, Month.AUGUST, 7, 15, 6, 1, 99);
    return List.of(
        new Security("BALN", 198.15, "CHF", timestamp, "Switzerland"),
        new Security("INGA", 18.95, "EUR", timestamp, "Amsterdam")
    );
  }

}