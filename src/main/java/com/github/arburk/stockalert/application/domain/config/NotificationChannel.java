package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationChannel(
    String type,
    String recipients,
    @JsonProperty("use-on-error")
    boolean useOnError
) {
}
