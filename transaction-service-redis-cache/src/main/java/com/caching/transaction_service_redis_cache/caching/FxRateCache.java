package com.caching.transaction_service_redis_cache.caching;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class FxRateCache {

  private static final String KEY_PREFIX = "fxRate:";

  private final RedisTemplate<String, BigDecimal> redisTemplate;
  private final Duration defaultTtl;

  public FxRateCache(
      RedisTemplate<String, BigDecimal> redisTemplate,
      @Value("${cache.fx-rate.ttl-seconds:60}") long ttlSeconds) {
    this.redisTemplate = redisTemplate;
    this.defaultTtl = Duration.ofSeconds(ttlSeconds);
  }

  public BigDecimal get(String key) {
    return redisTemplate.opsForValue().get(prefixed(key));
  }

  public void put(String key, BigDecimal value) {
    put(key, value, defaultTtl);
  }

  public void put(String key, BigDecimal value, Duration ttl) {
    redisTemplate.opsForValue().set(prefixed(key), value, ttl);
  }

  private String prefixed(String key) {
    return KEY_PREFIX + key;
  }
}
