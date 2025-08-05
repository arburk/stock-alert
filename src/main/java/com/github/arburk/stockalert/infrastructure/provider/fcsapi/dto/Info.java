package com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import org.springframework.stereotype.Service;

import java.util.List;

@ToString
@Getter
@Service
public class Info {
  private List<String> exchanges;
  private List<String> sectors;

  @JsonProperty("server_time")
  private String serverTime;

  @JsonProperty("credit_count")
  private int creditCount;

}
