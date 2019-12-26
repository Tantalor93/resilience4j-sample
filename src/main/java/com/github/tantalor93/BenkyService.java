package com.github.tantalor93;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class BenkyService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenkyService.class);
	private static final Random RANDOM = new Random();
	private static final String NAME = "BenkyService";

	@CircuitBreaker(name = NAME, fallbackMethod = "doItFallback")
	@RateLimiter(name = NAME)
	@Bulkhead(name = NAME)
	@Retry(name = NAME, fallbackMethod = "doItFallback")
	public long doIt() {
		LOGGER.info("I am doing it");
		return randomFail();
	}

	private long doItFallback(RuntimeException ex) {
		LOGGER.error("Fallback from error");
		return 0L;
	}

	static long randomFail() {
		long result = Math.abs(RANDOM.nextLong()) % 10;
		try {
			TimeUnit.SECONDS.sleep(result);
			if (result > 1) {
				LOGGER.error("Failed with result '{}'", result);
				throw new RuntimeException();
			}
			return result;
		} catch (Exception ex) {
			throw new RuntimeException();
		}
	}
}
