package com.github.arburk.stockalert.infrastructure.persistance.google.drive;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;

@Slf4j
@Service
@Configuration
@ConditionalOnProperty(value = "stock-alert.storage-provider", havingValue = GoogleDriveStorage.ENABLE_PROPERTY)
public class GoogleDriveService {

  @Value("${google-drive.service_config}")
  private String serviceConfig;

  @Value("${spring.application.name}")
  private String appName;

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  private Drive driveService;

  public Drive getGoogleDriveService() throws GeneralSecurityException, IOException {
    if (driveService == null) {
      driveService = initDriveService();
    }
    return driveService;
  }

  private Drive initDriveService() throws IOException, GeneralSecurityException {

    try (ByteArrayInputStream credentialsStream = new ByteArrayInputStream(getServiceConfig())) {
      final GoogleCredentials credentials = ServiceAccountCredentials.fromStream(credentialsStream)
          .createScoped(Collections.singleton(DriveScopes.DRIVE));

      return new Drive.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          JSON_FACTORY,
          new HttpCredentialsAdapter(credentials))
          .setApplicationName(appName)
          .build();
    }
  }

  byte[] getServiceConfig() {
    return Base64.getDecoder().decode(this.serviceConfig);
  }

}
