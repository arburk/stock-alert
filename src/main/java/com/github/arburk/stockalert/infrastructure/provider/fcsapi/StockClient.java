package com.github.arburk.stockalert.infrastructure.provider.fcsapi;

import com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto.StockApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "stockClient", url = "${stock-alert.base-url}",  configuration = FeignConfig.class)
public interface StockClient {

  @GetMapping("/latest")
  StockApiResponse getLatestStocks(
      @RequestParam("symbol") String symbols,
      @RequestParam("access_key") String apiKey
  );

}
