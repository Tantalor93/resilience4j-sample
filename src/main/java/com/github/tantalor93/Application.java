package com.github.tantalor93;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

@SpringBootApplication
public class Application implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	@Autowired
	private BenkyService benkyService;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
//		TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
//				.timeoutDuration(Duration.ofSeconds(4))
//				.build();
//
//		TimeLimiter timeLimiter = TimeLimiter.of("benkyRandom", timeLimiterConfig);
//
//		Callable<Long> longCallable = TimeLimiter
//				.decorateFutureSupplier(timeLimiter, () -> CompletableFuture.completedFuture(benkyService.doIt()));
//
//		LOGGER.info("done '{}'", longCallable.call());

		RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
				.limitForPeriod(1)
				.limitRefreshPeriod(Duration.ofSeconds(5))
				.build();

		RateLimiter rateLimiter = RateLimiter.of("benkyRandom", rateLimiterConfig);

		Supplier<Long> rateLimited = RateLimiter.decorateSupplier(rateLimiter, () -> benkyService.doIt());

		CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
				.failureRateThreshold(50)
				.slidingWindow(5, 3, CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
				.build();

		CircuitBreaker circuitBreaker = CircuitBreaker.of("benkyRandom", circuitBreakerConfig);

		Supplier<Long> circuitBraked = CircuitBreaker.decorateSupplier(circuitBreaker, rateLimited);

		RetryConfig retryConfig = RetryConfig.custom()
				.maxAttempts(10)
				.build();
		Retry retry = Retry.of("benkyRandom", retryConfig);

		Supplier<Long> call = Retry.decorateSupplier(retry, circuitBraked);

		long start = System.currentTimeMillis();
		Long result = null;
		try {
			result = call.get();
		} catch (Throwable ex) {
			LOGGER.error("caught exception", ex);
		}

		long duration = System.currentTimeMillis() - start;
		LOGGER.info("Result '{}' found in '{}' ms", result, duration);

		benkyService.doIt();
	}


}
