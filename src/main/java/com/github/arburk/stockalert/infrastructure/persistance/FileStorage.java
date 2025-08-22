package com.github.arburk.stockalert.infrastructure.persistance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.service.stock.PersistanceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(value = "stock-alert.storage-provider", havingValue = FileStorage.ENABLE_PROPERTY)
public class FileStorage implements PersistanceProvider {

  public static final String ENABLE_PROPERTY = "default";

  private final Path filePath;
  private final ObjectMapper objectMapper;

  private Collection<Security> data;

  public FileStorage(ObjectMapper objectMapper) {
    filePath = Path.of(System.getProperty("user.home"), "stock-alert", PersistanceProvider.STORAGE_FILE_NAME);
    this.objectMapper = objectMapper;
    log.debug("Initialized default PersistanceProvider");
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
          log.debug("Created directory '{}'.", parentDir.getAbsoluteFile());
        } else {
          log.warn("Failed to create directory '{}'.", parentDir.getAbsoluteFile());
        }
      }

      objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), data);
      log.info("Securities successfully updated to file: {}", filePath.toFile().getAbsoluteFile());
    } catch (IOException e) {
      log.error("Failed to write securities to file '{}}'", filePath.toFile().getAbsoluteFile(), e);
    }
  }

  private Collection<Security> initData() {
    try {
      if (!filePath.toFile().exists()) {
        log.warn("Storage file not found: {}", filePath.toFile().getAbsoluteFile());
        return initDataByFallback();
      }
      return objectMapper.readValue(filePath.toFile(), new TypeReference<List<Security>>() {});
    } catch (IOException e) {
      log.error("Failed to read securities from file", e);
      return new ArrayList<>();
    }
  }

  private Collection<Security> initDataByFallback() throws IOException {
    final Path fallback = Path.of(System.getProperty("user.home"), "stock-alert", PersistanceProvider.STORAGE_FILE_NAME_0_1_3);
    if (fallback.toFile().exists()) {
      log.info("init data from former storage file for migration: {}", fallback.toFile().getAbsoluteFile());
      return objectMapper.readValue(fallback.toFile(), new TypeReference<List<Security>>() {});
    }
    return new ArrayList<>();
  }

}
