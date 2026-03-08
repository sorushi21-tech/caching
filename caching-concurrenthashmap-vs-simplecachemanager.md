# ConcurrentHashMap vs SimpleCacheManager

---

## 1. Introduction to Caching

Caching is a technique whereby frequently accessed data is stored in a fast-access memory layer to avoid repeated computation or expensive I/O operations such as database queries or remote API calls.

- **Manual caching** using `java.util.concurrent.ConcurrentHashMap`
- **Declarative/managed caching** using Spring's `SimpleCacheManager`


---

## 2. Manual Caching with `ConcurrentHashMap`

### 2.1 Overview

`ConcurrentHashMap` is a thread-safe implementation of `Map` introduced in `java.util.concurrent`. 
- It uses segment-level locking (prior to Java 8) and lock-striping / CAS operations (Java 8+) to allow concurrent reads and writes without locking the entire map.

When used as a cache, `ConcurrentHashMap` acts as a simple key-value store held in the JVM heap.

### 2.2 Internal Mechanics

In Java 8 and later, `ConcurrentHashMap` is backed by an array of nodes. Each bucket in the array is either:

- A **linked list** (when the number of entries in that bucket is small)
- A **red-black tree** (`TreeBin`) when the bucket overflows (threshold: 8 entries)

>- Reads do not acquire locks — they rely on `volatile` reads of node references, making them lock-free and very fast. 
>- Writes use `synchronized` on individual bucket heads, limiting contention to a single bucket rather than the entire map.

### 2.3 Basic Usage as a Cache

```java
import java.util.concurrent.ConcurrentHashMap;

public class ProductCacheService {

    private final ConcurrentHashMap<Long, Product> cache = new ConcurrentHashMap<>();

    public Product getProduct(Long id, ProductRepository repository) {
        return cache.computeIfAbsent(id, repository::findById);
    }

    public void evict(Long id) {
        cache.remove(id);
    }

    public void evictAll() {
        cache.clear();
    }
}
```

`computeIfAbsent(key, mappingFunction)` is **atomic**: if the key is absent, the function is invoked and the result is stored — all as a single atomic operation with respect to other threads targeting the same key.

### 2.4 TTL (Time-To-Live)

`ConcurrentHashMap` has **no built-in expiry**. TTL must be implemented manually by wrapping cached values in a holder that tracks the insertion timestamp:

### 2.5 Size Limiting — Manual Implementation

`ConcurrentHashMap` is unbounded. To cap the cache size, the map size must be checked before each insertion:

### 2.6 Advantages

| Aspect                      | Detail                                                                 |
|-----------------------------|------------------------------------------------------------------------|
| **No framework dependency** | Works in plain Java with no Spring or third-party dependencies         |
| **Minimal overhead**        | Direct heap access; no proxy, reflection, or AOP overhead              |
| **Full control**            | Eviction logic, TTL, and size limits are under the developer's control |
| **Fast reads**              | Lock-free volatile reads on uncontested buckets                        |

### 2.7 Disadvantages

| Aspect                                             | Detail                                                             |
|----------------------------------------------------|--------------------------------------------------------------------|
| **No TTL/eviction out of the box**                 | Must be implemented manually                                       |
| **No statistics/monitoring**                       | No hit rate, miss rate, or eviction count metrics                  |
| **Unbounded memory usage**                         | Can grow without limit if entries are not explicitly removed       |
| **Not integrated with Spring caching abstraction** | Cannot use `@Cacheable`, `@CacheEvict`, or `@CachePut` annotations |
| **Cache-aside pattern only**                       | All cache logic must be manually coded at the call site            |

---

## 3. Declarative Caching with `SimpleCacheManager`

### 3.1 Overview

`SimpleCacheManager` is part of Spring Framework's **Cache Abstraction** (`org.springframework.cache`). 
- It acts as a registry of named `Cache` instances. The underlying storage for each named cache is typically a `ConcurrentMapCache` — which itself wraps a `ConcurrentHashMap` — but the abstraction decouples the caching logic from the business code via annotations.

### 3.2 The Spring Cache Abstraction

Spring's cache abstraction introduces the following annotations:

| Annotation     | Purpose                                                                         |
|----------------|---------------------------------------------------------------------------------|
| `@Cacheable`   | Caches the return value of a method. On a cache hit, the method is not invoked. |
| `@CachePut`    | Always invokes the method and updates the cache with the return value.          |
| `@CacheEvict`  | Removes one or all entries from a named cache.                                  |
| `@Caching`     | Groups multiple cache annotations on a single method.                           |
| `@CacheConfig` | Shares common cache configuration at the class level.                           |

These annotations are processed by Spring AOP. A proxy wraps the target bean, intercepting calls and delegating to the configured `CacheManager`.

### 3.3 Configuration

#### Java Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
            new ConcurrentMapCache("products"),
            new ConcurrentMapCache("orders")
        ));
        return manager;
    }
}
```

`@EnableCaching` activates the Spring Cache AOP proxy infrastructure. Without it, the annotations are parsed but no caching occurs.

### 3.4 `ConcurrentMapCache` Internals

`SimpleCacheManager` by default uses `ConcurrentMapCache`, which wraps a `ConcurrentHashMap<Object, Object>`. 
- The `ConcurrentMapCache` stores values **by reference** (no serialization), meaning:
  - Cached objects are shared across callers; mutating a returned object mutates the cached copy.
  - There is no TTL, no size limit, and no eviction — identical to raw `ConcurrentHashMap` usage.

### 3.8 Advantages

| Aspect                    | Detail                                                                                                         |
|---------------------------|----------------------------------------------------------------------------------------------------------------|
| **Declarative**           | Business logic is free of caching boilerplate                                                                  |
| **Named caches**          | Multiple logically separated caches under one manager                                                          |
| **AOP proxy integration** | Works seamlessly with Spring's transaction and security proxies                                                |
| **Testability**           | Caching can be disabled in tests by removing `@EnableCaching`                                                  |

### 3.9 Disadvantages

| Aspect                                      | Detail                                                                                                 |
|---------------------------------------------|--------------------------------------------------------------------------------------------------------|
| **No TTL/eviction in `SimpleCacheManager`** | `ConcurrentMapCache` has no expiry or size limit                                                       |
| **Proxy-based limitation**                  | Self-invocation within the same bean bypasses the AOP proxy; `@Cacheable` is not triggered             |
| **Null value handling**                     | `ConcurrentMapCache` stores null values by default but this can cause `NullPointerException` surprises |
| **No statistics**                           | No built-in metrics for hit/miss ratio                                                                 |

---

## 4. Self-Invocation Problem

A critical limitation of Spring's AOP-based caching is that calling a `@Cacheable` method from within the **same bean** does not go through the proxy and thus does not trigger caching:

```java
@Service
public class ProductService {

    // ❌ WRONG: Direct internal call bypasses AOP proxy
    public void refresh(Long id) {
        getProduct(id); // Cache NOT populated
    }

    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        return repository.findById(id);
    }
}
```

**Solutions:**

1. Inject the service into itself via `@Autowired` (self-injection):

```java
@Autowired
private ProductService self;

public void refresh(Long id) {
    self.getProduct(id); // Goes through the proxy ✅
}
```

2. Extract the `@Cacheable` method into a separate Spring-managed bean.

3. Use `AopContext.currentProxy()` with `@EnableAspectJAutoProxy(exposeProxy = true)`.

---

## 5. Side-by-Side Comparison

| Feature                     | `ConcurrentHashMap` (manual)     | `SimpleCacheManager` (Spring)                       |
|-----------------------------|----------------------------------|-----------------------------------------------------|
| **Thread safety**           | ✅ Built-in                       | ✅ Via `ConcurrentMapCache`                          |
| **TTL / Expiry**            | ❌ Manual implementation required | ❌ Not in `SimpleCacheManager`; use Caffeine         |
| **Max size / eviction**     | ❌ Manual implementation required | ❌ Not in `SimpleCacheManager`; use Caffeine         |
| **Statistics**              | ❌ None                           | ❌ None (use Caffeine with `recordStats()`)          |
| **Spring annotations**      | ❌ Not supported                  | ✅ `@Cacheable`, `@CacheEvict`, `@CachePut`          |
| **Framework dependency**    | ❌ None (plain Java)              | ✅ Requires Spring context                           |
| **Swappable backend**       | ❌ Tightly coupled                | ✅ Swap to Redis/Caffeine with one bean              |
| **Multiple named caches**   | Manual naming                    | ✅ Built-in named cache registry                     |
| **Null value support**      | ✅ Native                         | ✅ Configurable                                      |
| **Self-invocation support** | ✅ Works                          | ❌ AOP proxy limitation                              |
| **Testing**                 | Simple unit test                 | Requires Spring context or `@EnableCaching` removal |

---

## 7. When to Use Which

### Use `ConcurrentHashMap` directly when:

- The application is a plain Java service with no Spring context.
- Fine-grained, custom eviction logic is required that doesn't fit standard TTL/LRU patterns.
- The cache is very small and tightly controlled, with explicit eviction calls.
- AOP proxy overhead is a concern (e.g., extremely high-frequency calls in tight loops).

### Use `SimpleCacheManager` (or `CaffeineCacheManager`) when:

- The application is a Spring Boot service and the team wants clean, annotation-driven caching.
- The caching strategy may change in the future (e.g., migrating from in-memory to Redis).
- Multiple named caches with different configurations are needed.
- Standard TTL/size eviction is acceptable (use Caffeine underneath).
- Monitoring and cache statistics are a requirement.

---