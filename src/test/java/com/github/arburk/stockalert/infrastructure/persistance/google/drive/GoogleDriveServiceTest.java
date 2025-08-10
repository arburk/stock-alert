package com.github.arburk.stockalert.infrastructure.persistance.google.drive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"stock-service-int-test"})
@TestPropertySource(properties = {
    "stock-alert.storage-provider=google-drive",
    "google-drive.service_config=ew0KICAidHlwZSI6ICJzZXJ2aWNlX2FjY291bnQiLA0KICAicHJvamVjdF9pZCI6ICJzdG9jay1hbGVydC0xMjM0NTYiLA0KICAicHJpdmF0ZV9rZXlfaWQiOiAiMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwYWEiLA0KICAicHJpdmF0ZV9rZXkiOiAiLS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tXG5HTlFkbkE1ZFVWY2JreFVPR1paXG4tLS0tLUVORCBQUklWQVRFIEtFWS0tLS0tXG4iLA0KICAiY2xpZW50X2VtYWlsIjogInN0b2NrLWFsZXJ0LXN0b3JhZ2UtcHJvdmlkZXJAaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLA0KICAiY2xpZW50X2lkIjogIjk5OTk5OTk5OTk5OTk5OTk5OTkiLA0KICAiYXV0aF91cmkiOiAiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tL28vb2F1dGgyL2F1dGgiLA0KICAidG9rZW5fdXJpIjogImh0dHBzOi8vb2F1dGgyLmdvb2dsZWFwaXMuY29tL3Rva2VuIiwNCiAgImF1dGhfcHJvdmlkZXJfeDUwOV9jZXJ0X3VybCI6ICJodHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9vYXV0aDIvdjEvY2VydHMiLA0KICAiY2xpZW50X3g1MDlfY2VydF91cmwiOiAiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vcm9ib3QvdjEvbWV0YWRhdGEveDUwOS9zdG9jay1hbGVydC1zdG9yYWdlLXByb3ZpZGVyLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwNCiAgInVuaXZlcnNlX2RvbWFpbiI6ICJnb29nbGVhcGlzLmNvbSINCn0NCg==",
})
class GoogleDriveServiceTest {

  @Autowired
  private GoogleDriveService googleDriveService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void getServiceConfig() throws IOException {
    final byte[] serviceConfig = googleDriveService.getServiceConfig();
    assertNotNull(serviceConfig);

    final String json = new String(serviceConfig);
    assertTrue(json.contains("\"type\": \"service_account\""));
    assertTrue(json.contains("\"project_id\": \"stock-alert-123456\""));
    assertTrue(json.contains("\"private_key_id\": \"123456789012345678901234567890aa\""));
    assertTrue(json.contains("\"private_key\": \"-----BEGIN PRIVATE KEY-----\\nGNQdnA5dUVcbkxUOGZZ\\n-----END PRIVATE KEY-----\\n\""));
    assertTrue(json.contains("\"client_email\": \"stock-alert-storage-provider@iam.gserviceaccount.com\""));
    assertTrue(json.contains("\"client_id\": \"9999999999999999999\""));
    assertTrue(json.contains("\"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\""));
    assertTrue(json.contains("\"token_uri\": \"https://oauth2.googleapis.com/token\""));
    assertTrue(json.contains("\"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\""));
    assertTrue(json.contains("\"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/stock-alert-storage-provider.iam.gserviceaccount.com\""));
    assertTrue(json.contains("\"universe_domain\": \"googleapis.com\""));

    final JsonNode testInput = objectMapper.readTree(serviceConfig);
    final JsonNode expected = objectMapper.readTree(ClassLoader.getSystemResourceAsStream("storage/google/test-config.json"));
    assertEquals(expected, testInput);
  }

}