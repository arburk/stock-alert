package com.github.arburk.stockalert.application.service.stock;

/**
 * Signals that the configured {@link PersistenceProvider} could not read or write its storage
 * (e.g. S3 bucket unreachable, credentials rejected, file not readable).
 *
 * <p>Distinct from a legitimately missing storage file (first run), which is <b>not</b> an error.
 * Thrown so a run aborts before alerts are evaluated against incomplete state and before an empty
 * state overwrites the persisted one.
 */
public class StorageException extends RuntimeException {

  public StorageException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
