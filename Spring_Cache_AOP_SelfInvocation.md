# Spring Caching & AOP — The Self-Invocation Problem

> **@Cacheable • @CachePut • @CacheEvict | Proxy Mechanism, Root Cause & All Solutions**

---

## 1. Foundation — How Spring Caching Works Under the Hood

- Spring's declarative caching annotations (`@Cacheable`, `@CachePut`, `@CacheEvict`) are not implemented inside the bean itself. They are implemented by **Spring AOP** — specifically, by wrapping the bean inside a **proxy object** at application startup. 
- Every call that arrives from outside the bean goes through this proxy, which intercepts the call, executes the caching logic, and then delegates to the real bean method.

This architectural decision is what gives Spring its clean, non-intrusive programming model — business logic remains free of infrastructure concerns. 
- However, it carries a fundamental constraint: **the proxy only intercepts calls that cross the proxy boundary.**

### 1.1 The Proxy Wrapper — Conceptual Model

- When a Spring bean is annotated with any cache annotation, the Spring container does not return the actual bean instance to callers. 
- Instead, it returns a **proxy** that wraps the actual bean.

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Spring Application Context                   │
│                                                                      │
│   External Caller                                                    │
│        │                                                             │
│        ▼                                                             │
│   ┌─────────────────────────────┐                                    │
│   │       PROXY (AOP wrapper)   │   ◄── What callers see             │
│   │  ┌───────────────────────┐  │                                    │
│   │  │  CacheInterceptor     │  │   ◄── Checks cache, applies logic  │
│   │  └───────────┬───────────┘  │                                    │
│   │              │ delegates    │                                    │
│   │  ┌───────────▼───────────┐  │                                    │
│   │  │   REAL Bean (target)  │  │   ◄── Your @Service class          │
│   │  └───────────────────────┘  │                                    │
│   └─────────────────────────────┘                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### 1.2 What the CacheInterceptor Does on Every Intercepted Call

For a `@Cacheable` method, the interceptor performs the following logic on every call that comes through the proxy:

1. Compute the cache key from method arguments (using a `KeyGenerator`).
2. Look up the key in the named `CacheManager` / `Cache`.
3. **Cache hit** → return the cached value immediately (method body never executes).
4. **Cache miss** → invoke the real method, store the result in the cache, return the result.

This entire flow is **bypassed** when the proxy is not in the call path — which is precisely what happens during self-invocation.

---

## 2. The Self-Invocation Problem — Root Cause Analysis

### 2.1 What Is Self-Invocation?

- Self-invocation (also called an *internal method call*) occurs when a method within a bean calls another method on the **same bean instance** using the implicit `this` reference. 
- In Java, this resolves to a direct method call on the concrete object — completely bypassing any proxy wrapping that Spring may have applied.

### 2.2 The Broken Call Path

```java
@Service
public class ProductService {

    // Method A — calls Method B internally
    public void refreshAll() {
        evictProductCache();          // ← self-invocation via 'this'
    }

    @CacheEvict(value = "products", allEntries = true)
    public void evictProductCache() { // ← @CacheEvict is IGNORED
        System.out.println("Evicting cache...");
    }
}
```

```
External Caller
       │
       ▼
  [PROXY] ──► refreshAll()    (no cache annotation → passes through)
                  │
                  │  this.evictProductCache()   ← direct call on 'this'
                  │  BYPASSES PROXY ENTIRELY
                  ▼
          [REAL BEAN].evictProductCache()        ← @CacheEvict is IGNORED
```

When `refreshAll()` is called from outside, the proxy intercepts it. 
    
- Since `refreshAll()` has no cache annotation, the proxy simply delegates to the real bean. 
  - Inside the real bean, `evictProductCache()` is called via `this` — a direct JVM method call. 
  - **The proxy is never involved**, so the `@CacheEvict` annotation has no effect.

### 2.3 The Core Rule

> **The Golden Rule**
>
> - Spring AOP only intercepts method calls that **enter the bean through the proxy**.
> - Any method called via `this` (i.e., within the same bean instance) bypasses the proxy entirely.
> - All cache annotations on such methods are **completely silenced**.
>
> This applies equally to `@Cacheable`, `@CachePut`, and `@CacheEvict`.

---

## 3. Annotation-by-Annotation Failure Scenarios

### 3.1 @Cacheable — Method Always Executes, Cache Never Populated

```java
@Service
public class UserService {

    public UserDto getUser(Long id) {
        // Calls getUserFromDb() via 'this' — cache is never consulted
        return getUserFromDb(id);
    }

    @Cacheable(value = "users", key = "#id")
    public UserDto getUserFromDb(Long id) {   // ← always executes, never cached
        return repository.findById(id).orElseThrow();
    }
}
```

**Consequence:** 
- The database is hit on every invocation of `getUser()` regardless of how many times the same ID is requested. 
- The cache remains perpetually empty for this code path.

---

### 3.2 @CachePut — Method Executes but Cache Is Never Updated

```java
@Service
public class OrderService {

    public Order processOrder(Order order) {
        Order saved = repository.save(order);
        updateOrderCache(saved);  // ← self-invocation: cache NOT updated
        return saved;
    }

    @CachePut(value = "orders", key = "#order.id")
    public Order updateOrderCache(Order order) {  // ← executes, but @CachePut ignored
        return order;
    }
}
```

**Consequence:** Order is saved to the database but stale data remains in the cache. The next cache hit will return the old version of the order.

---

### 3.3 @CacheEvict — Stale Data Persists Indefinitely

```java
@Service
public class CatalogService {

    @Transactional
    public void updateProduct(Product p) {
        repository.save(p);
        clearProductCache(p.getId());  // ← self-invocation: eviction skipped
    }

    @CacheEvict(value = "catalog", key = "#id")
    public void clearProductCache(Long id) {  // ← @CacheEvict is silenced
    }
}
```

**Consequence:** The product is updated in the database, but the cache still holds the old product. Every read until cache expiry returns stale data.

---

## 4. Solutions — Six Approaches with Trade-offs

### Solution 1: Split Into Separate Beans (Recommended)

- The cleanest architectural solution is to eliminate self-invocation by moving the cached methods to a **different bean**. 
- When `BeanA` calls a method on `BeanB`, the call goes through BeanB's proxy, and all cache annotations are honoured.

```java
@Service
public class UserService {
    private final UserCacheService userCacheService;

    public UserService(UserCacheService userCacheService) {
        this.userCacheService = userCacheService;
    }

    public UserDto getUser(Long id) {
        // External call through proxy — @Cacheable IS applied
        return userCacheService.getUserFromDb(id);
    }
}

@Service
public class UserCacheService {

    @Cacheable(value = "users", key = "#id")
    public UserDto getUserFromDb(Long id) {   // ← cache works correctly
        return repository.findById(id).orElseThrow();
    }
}
```

> **Best Practice:** Splitting beans is the most idiomatic Spring approach. It promotes single-responsibility, makes the caching concern explicit, and is the easiest to test and reason about.

---

###  Solution 2: Self-Injection via @Autowired

A bean can inject a reference to its own **proxy** by autowiring itself. All calls made through this self-reference will go through the proxy.

```java
@Service
public class ProductService {

    @Autowired
    private ProductService self;  // Spring injects the proxy, not 'this'

    public void refreshAll() {
        self.evictProductCache();  // goes through proxy → @CacheEvict fires
    }

    @CacheEvict(value = "products", allEntries = true)
    public void evictProductCache() {
        // executes AND cache is evicted
    }
}
```

Alternatively, retrieve the proxy programmatically:

```java
// Requires: @EnableAspectJAutoProxy(exposeProxy = true)
((ProductService) AopContext.currentProxy()).evictProductCache();
```

**Trade-off:** Self-injection is a code smell — it creates circular dependency confusion. Use sparingly.

---


## Key Takeaways

- Spring cache annotations work through **proxy-based AOP interception** — they only fire on calls that cross the proxy boundary from the outside.
- Any method called via `this` bypasses the proxy. The annotation is present in source code but is **completely ineffective at runtime**.
- This is **not a bug** — it is an inherent limitation of proxy-based AOP. The same limitation applies to `@Transactional`, `@Async`, and any other Spring AOP-based annotation.
- The **recommended fix** is splitting responsibilities into separate beans, ensuring that inter-bean calls always travel through the proxy.