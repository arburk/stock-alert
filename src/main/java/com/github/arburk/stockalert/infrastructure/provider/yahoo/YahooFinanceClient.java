package com.github.arburk.stockalert.infrastructure.provider.yahoo;

import com.github.arburk.stockalert.infrastructure.provider.yahoo.dto.ChartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "yahooFinanceClient", url = "${stock-alert.base-url}", configuration = FeignConfig.class)
public interface YahooFinanceClient {

  @GetMapping("/v8/finance/chart/{symbol}")
  ChartResponse getChart(
      @PathVariable("symbol") String symbol,
      @RequestParam("interval") String interval,
      @RequestParam("range") String range
  );

}
