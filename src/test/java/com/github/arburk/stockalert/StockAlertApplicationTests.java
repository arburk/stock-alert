package com.github.arburk.stockalert;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles(profiles = "test")
@TestPropertySource(locations = "classpath:application-test.yml")
class StockAlertApplicationTests {

	@Test
	void contextLoads() {	}

}
