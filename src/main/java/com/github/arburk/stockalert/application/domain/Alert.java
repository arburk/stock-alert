package com.github.arburk.stockalert.application.domain;

import java.time.LocalDateTime;

public record Alert(
    LocalDateTime timestamp,
    Double threshold,
    String unit
) implements Comparable<Alert> {

  @Override
  public int compareTo(final Alert other) {
    if (this.timestamp == null && other != null && other.timestamp == null) {
      return 0;
    }
    if (this.timestamp == null &&  other != null) {
      return -1;
    }
    return (other == null || other.timestamp == null)
        ? 1
        : other.timestamp.compareTo(this.timestamp); // reversed order
  }
}
