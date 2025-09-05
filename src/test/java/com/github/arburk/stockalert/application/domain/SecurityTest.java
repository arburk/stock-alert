package com.github.arburk.stockalert.application.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityTest {

  @Test
  void addLogSameTypeNotDuplicated() {
    final LocalDateTime now = LocalDateTime.now();
    final LocalDateTime oneHorAgo = now.minusHours(1);

    final Security testee = new Security(null, null, null, null, null, null, null);
    assertTrue(testee.alertLog().isEmpty());

    final Alert alert1 = new Alert(oneHorAgo, 1., "CHF");

    IntStream.of(3).forEach(run -> {
      testee.addLog(alert1);
      assertEquals(1, testee.alertLog().size());
      assertTrue(testee.alertLog().contains(alert1));
    });

    final Alert alert2 = new Alert(oneHorAgo, 1.1, "CHF");
    testee.addLog(alert2);
    assertEquals(2, testee.alertLog().size());
    assertTrue(testee.alertLog().contains(alert1));
    assertTrue(testee.alertLog().contains(alert2));

    final Alert copyOfAlert1 = new Alert(now, alert1.threshold(), alert1.unit());
    testee.addLog(copyOfAlert1);
    assertEquals(2, testee.alertLog().size());
    assertTrue(testee.alertLog().contains(copyOfAlert1));
    assertTrue(testee.alertLog().contains(alert2));

    final Alert result = testee.alertLog().stream().filter(log -> log.equals(alert1)).toList().getFirst();
    assertEquals(copyOfAlert1.timestamp(), result.timestamp());
    assertNotEquals(alert1.timestamp(), result.timestamp());
  }
}