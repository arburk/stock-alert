package com.github.arburk.stockalert;

import com.github.arburk.stockalert.application.service.stock.PersistanceProvider;
import com.github.arburk.stockalert.infrastructure.persistance.FileStorage;
import com.github.arburk.stockalert.infrastructure.persistance.google.drive.GoogleDriveStorage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StockAlertApplicationTests {

  @Nested
  @SpringBootTest
  @ActiveProfiles(profiles = "test")
  @TestPropertySource(locations = "classpath:application-test.yml")
  class Defaults {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void contextLoads() {
      assertInstanceOf(FileStorage.class, context.getBean(PersistanceProvider.class));
    }
  }

  @Nested
  @SpringBootTest
  @ActiveProfiles(profiles = "test")
  @TestPropertySource(locations = "classpath:application-test.yml", properties = {
      "stock-alert.storage-provider=google-drive",
      "google-drive.service_config=ew0KICAidHlwZSI6ICJzZXJ2aWNlX2FjY291bnQiLA0KICAicHJvamVjdF9pZCI6ICJzdG9jay1hbGVydC0xMjM0NTYiLA0KICAicHJpdmF0ZV9rZXlfaWQiOiAiMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwYWEiLA0KICAicHJpdmF0ZV9rZXkiOiAiLS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tXG5HTlFkbkE1ZFVWY2JreFVPR1paXG4tLS0tLUVORCBQUklWQVRFIEtFWS0tLS0tXG4iLA0KICAiY2xpZW50X2VtYWlsIjogInN0b2NrLWFsZXJ0LXN0b3JhZ2UtcHJvdmlkZXJAaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLA0KICAiY2xpZW50X2lkIjogIjk5OTk5OTk5OTk5OTk5OTk5OTkiLA0KICAiYXV0aF91cmkiOiAiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tL28vb2F1dGgyL2F1dGgiLA0KICAidG9rZW5fdXJpIjogImh0dHBzOi8vb2F1dGgyLmdvb2dsZWFwaXMuY29tL3Rva2VuIiwNCiAgImF1dGhfcHJvdmlkZXJfeDUwOV9jZXJ0X3VybCI6ICJodHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9vYXV0aDIvdjEvY2VydHMiLA0KICAiY2xpZW50X3g1MDlfY2VydF91cmwiOiAiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vcm9ib3QvdjEvbWV0YWRhdGEveDUwOS9zdG9jay1hbGVydC1zdG9yYWdlLXByb3ZpZGVyLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwNCiAgInVuaXZlcnNlX2RvbWFpbiI6ICJnb29nbGVhcGlzLmNvbSINCn0NCg==",
  })
  class GoogleStorage {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void contextLoads() {
      assertInstanceOf(GoogleDriveStorage.class, context.getBean(PersistanceProvider.class));
    }
  }
}
