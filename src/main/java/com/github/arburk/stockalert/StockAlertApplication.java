package com.github.arburk.stockalert;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.service.Scheduler;
import com.github.arburk.stockalert.infrastructure.persistance.google.drive.GoogleDriveService;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.HashMap;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "com.github.arburk.stockalert.infrastructure.provider")
public class StockAlertApplication {

	public static void main(String[] args) {
    final ConfigurableApplicationContext app = SpringApplication.run(StockAlertApplication.class, args);
    init(app);
  }

  private static void init(final ConfigurableApplicationContext app) {
    final ApplicationConfig config = app.getBean(ApplicationConfig.class);
    debug(app);
    log.info("StockAlertConfig: {}", config);
    if (config.isRunOnStartup()) {
      app.getBean(Scheduler.class).updateStock();
    }
  }

  private static void debug(final ConfigurableApplicationContext app) {
    final GoogleDriveService bean = app.getBean(GoogleDriveService.class);
    try {
      final Drive googleDriveService = bean.getGoogleDriveService();
      log.debug(googleDriveService.toString());
      var files = googleDriveService.files().list()
          .setPageSize(10)
          .execute();
      final HashMap<String, File> appFiles = new HashMap<>();
      files.getFiles().stream()
          .peek(file -> log.debug("{} ({}) -> {}", file.getName(), file.getId(), file.getMimeType()))
          .filter(file -> MimeTypeUtils.APPLICATION_JSON_VALUE.equals(file.getMimeType()))
          .forEach(file -> {
            switch (file.getName()) {
              case "config.json" -> appFiles.put("config", file);
              case "securities.db" -> appFiles.put("storage", file);
              default -> log.info("ignore {} ({})", file.getName(), file.getId());
            }
          });

      if (!appFiles.containsKey("storage")) {
        final DbFileStorage file = getFile();
        googleDriveService.files().create(file.file, file.contentsStream).execute();
        log.info("ignore {} ({})", file.file.getName(), file.file.getId());
        appFiles.put("storage", file.file);
      }
    } catch (GeneralSecurityException | IOException e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }

  }

  private static DbFileStorage getFile() {
    final File file = new File();
    file.setName("securities.db");
    file.setMimeType(MimeTypeUtils.APPLICATION_JSON_VALUE);
    final FileContent contentsStream = new FileContent(MimeTypeUtils.APPLICATION_JSON_VALUE, Path.of("C:\\Users\\arnob\\stock-alert", "securities.db").toFile());
    return new DbFileStorage(file, contentsStream);
  }

  private record DbFileStorage(File file, FileContent contentsStream) {
  }

}
