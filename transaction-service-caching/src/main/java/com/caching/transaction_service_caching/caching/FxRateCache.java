package com.caching.transaction_service_caching.caching;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class FxRateCache<K, V> {

  private final ConcurrentHashMap<K, V> fxRateCache = new ConcurrentHashMap<>();

  public V get(K key) {
    return fxRateCache.get(key);
  }

  public void put(K key, V value) {
    fxRateCache.put(key, value);
  }

  public void clear() {
    fxRateCache.clear();
  }
}
