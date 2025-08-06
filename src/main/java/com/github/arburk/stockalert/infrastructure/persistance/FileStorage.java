package com.github.arburk.stockalert.infrastructure.persistance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.service.PersistanceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class FileStorage implements PersistanceProvider {

  private final Path filePath;
  private final ObjectMapper objectMapper;

  private Collection<Security> data;

  public FileStorage(ObjectMapper objectMapper) {
    filePath = Path.of(System.getProperty("user.home"), "stock-alert", "securities.db");
    this.objectMapper = objectMapper;
  }

  @Override
  public Collection<Security> getSecurites() {
    if (data == null || data.isEmpty()) {
      data = initData();
    }
    return new ArrayList<>(data);
  }

  @Override
  public void updateSecurities(final Collection<Security> securities) {
    if (securities == null || securities.isEmpty()) {
      log.warn("update was called with empty securities. skip to prevent data loss.");
      log.info("If you want to reset data, stop the application and delete storage file '{}'.", filePath);
      return;
    }

    securities.forEach(latest -> {
      getSecurites().stream().filter(security ->
              security.symbol().equals(latest.symbol()) && security.exchange().equals(latest.exchange()))
          .findFirst().ifPresent(data::remove);
      data.add(latest);
    });

    persistInFile();
  }

  private void persistInFile() {
    try {
      final File parentDir = filePath.toFile().getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        if (parentDir.mkdirs()) {
          log.debug("Created directory '{}'.", parentDir);
        } else {
          log.warn("Failed to create directory '{}'.", parentDir);
        }
      }

      objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), data);
      log.info("Securities successfully updated to file: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to write securities to file '{}}'", filePath, e);
    }
  }

  private Collection<Security> initData() {
    try {
      if (!filePath.toFile().exists()) {
        log.warn("Storage file not found: {}", filePath);
        return new ArrayList<>();
      }
      return objectMapper.readValue(filePath.toFile(), new TypeReference<List<Security>>() {});
    } catch (IOException e) {
      log.error("Failed to read securities from file", e);
      return new ArrayList<>();
    }
  }
}
