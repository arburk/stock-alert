package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.domain.Security;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock
@ActiveProfiles({"stock-service-int-test"})
@TestPropertySource(properties = {
    "stock-alert.base-url=${wiremock.server.baseUrl}",
})
class StockServiceIntegrationTest {

  private static final String userHome = System.getProperty("user.home");
  private static final Path TEST_HOME = Paths.get("target/test/", String.valueOf(System.currentTimeMillis()));

  private static GreenMail greenMail;

  @BeforeAll
  static void setUpAll() {
    System.setProperty("user.home", TEST_HOME.toString()); // redirect file storage to test
    greenMail = new GreenMail(new ServerSetup(0 /* 0 = random port */, null, ServerSetup.PROTOCOL_SMTP));
    greenMail.setUser("test-user", "test-pass"); // defined in src/test/resources/application-stock-service-int-test.yml
    greenMail.start();
    System.setProperty("spring.mail.port", String.valueOf(greenMail.getSmtp().getPort()));
  }

  @AfterAll
  static void tearDownAll() {
    System.setProperty("user.home", userHome);  // rest user.home after test to default
    greenMail.stop();
  }

  @Autowired
  private StockService stockService;

  @Autowired
  private PersistanceProvider persistanceProvider;

  @Test
  void happyFlow_WithAlerting() throws MessagingException, IOException {
    stubFor(get("/latest?symbol=ENI%2CFRO%2CPPGN%2CSUNN%2CMBLY%2CMRK%2CSIKA%2CALV%2CBSLN%2CPFE%2CSRAIL%2CVODI%2CLEHN%2CINGA%2CHELN%2CBALN%2CSTMN%2CVACD%2CCMBN%2CAMRZ%2CROG%2CMBTN%2CMMM%2CVUL%2CBANB%2CBIOV%2CBAER%2CMOZN%2CDTE%2CLEON%2CDKSH%2CDVN&access_key=API_KEY")
        .willReturn(okJson(extendedResponse())));
    persistanceProvider.updateSecurities(/* peristed file required for comparison */ getPreparedSecurities());

    stockService.update();

    final Collection<Security> securites = persistanceProvider.getSecurites();
    assertEquals(32, securites.size());
    final Security baln = securites.stream().filter(security -> "BALN".equals(security.symbol())).findFirst().get();
    assertEquals(207.4, baln.price());

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
    assertEquals(1, receivedMessages.length);
    final MimeMessage receivedMessage = receivedMessages[0];
    assertEquals("Threshold CHF 200.0 for BALN crossed", receivedMessage.getSubject());
    List<String> expectedLines = List.of(
        "Price for BALN moved from CHF 198.15 dated on 2025-08-07 15:06 to CHF 207.4 dated on 2025-08-07 08:35",
        "Data refers to stock exchange Switzerland."
    );
    assertLinesMatch(expectedLines, Arrays.asList(receivedMessage.getContent().toString().split("\\R")));
  }

  private Collection<Security> getPreparedSecurities() {
    // For BALN a threshold of 200 is defined in src/test/resources/config/integration-test-config.json
    // so we set a value lower (198.15) here and a value higher (207.4) in src/test/resources/rest-client/extended-response.json
    // to trigger alert
    final LocalDateTime timestamp = LocalDateTime.of(2025, Month.AUGUST, 7, 15, 6, 1, 99);
    return List.of(new Security("BALN", 198.15, "CHF", timestamp, "Switzerland"));
  }

  private static String extendedResponse() {
    final File mockedFile = Path.of("src/test/resources/rest-client/extended-response.json").toFile();
    return Files.contentOf(mockedFile, StandardCharsets.UTF_8);
  }
}