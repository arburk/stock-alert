package com.github.arburk.stockalert.infrastructure.persistance;

import com.github.arburk.stockalert.application.domain.StockAlertDb;
import com.github.arburk.stockalert.application.service.stock.PersistenceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

@Slf4j
@Component
@ConditionalOnProperty(value = "stock-alert.storage-provider", havingValue = FileStorage.ENABLE_PROPERTY)
public class FileStorage extends AbstractPersistenceProvider implements PersistenceProvider {

  public static final String ENABLE_PROPERTY = "default";

  private final Path filePath;
  private final ObjectMapper objectMapper;

  public FileStorage(ObjectMapper objectMapper) {
    filePath = Path.of(System.getProperty("user.home"), "stock-alert", PersistenceProvider.STORAGE_FILE_NAME);
    this.objectMapper = objectMapper;
    log.debug("Initialized default PersistanceProvider");
  }

  @Override
  public void commitChanges() {
    try {
      final File parentDir = filePath.toFile().getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        if (parentDir.mkdirs()) {
          log.debug("Created directory '{}'.", parentDir.getAbsoluteFile());
        } else {
          log.warn("Failed to create directory '{}'.", parentDir.getAbsoluteFile());
        }
      }

      final StockAlertDb data = getData();
      final StockAlertDb copy = new StockAlertDb(data.securities() /* sort securities before saving*/, data.metaInfo());
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), copy);
      log.info("Securities successfully updated to file: {}", filePath.toFile().getAbsoluteFile());
    } catch (Exception e) {
      log.error("Failed to write securities to file '{}}'", filePath.toFile().getAbsoluteFile(), e);
    }
  }

  @Override
  StockAlertDb initData() {
    try {
      if (!filePath.toFile().exists()) {
        log.warn("Storage file not found: {}", filePath.toFile().getAbsoluteFile());
        return initDataByFallback();
      }
      return objectMapper.readValue(filePath.toFile(), StockAlertDb.class);
    } catch (Exception e) {
      log.error("Failed to read securities from file", e);
      return new StockAlertDb(new ArrayList<>(/* must not be immutable */), null);
    }
  }

  private StockAlertDb initDataByFallback() throws JacksonException {
    final Path fallback = Path.of(System.getProperty("user.home"), "stock-alert", PersistenceProvider.STORAGE_FILE_NAME_0_1_3);
    if (fallback.toFile().exists()) {
      log.info("init data from former storage file for migration: {}", fallback.toFile().getAbsoluteFile());
      return new StockAlertDb(objectMapper.readValue(fallback.toFile(), new TypeReference<>() {
      }), null);
    }
    return new StockAlertDb(new ArrayList<>(/* must not be immutable */), null);
  }

}
