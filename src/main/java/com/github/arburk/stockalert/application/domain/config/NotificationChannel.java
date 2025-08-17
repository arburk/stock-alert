package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationChannel {

  private String type;
  private String recipients;

  @JsonProperty("use-on-error")
  private boolean useOnError;
}
