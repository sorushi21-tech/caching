# Distributed Caching with Redis

---

## 1. What is Caching?

- A **cache** is a temporary storage layer that holds copies of frequently accessed data. 
- When an application needs data, it first checks the cache. If the data is found there, it is returned immediately without going to the main database. This is called a **cache hit**. If the data is not found, the application fetches it from the database and optionally stores it in the cache for future requests. This is called a **cache miss**.

The fundamental goal of caching is to reduce latency and decrease the load on the primary data store.

---

## 2. What is Distributed Caching?

In a single-server application, a local in-memory cache works well. However, in a distributed system where multiple application servers run in parallel, each server would have its own isolated local cache. This creates inconsistency — one server might hold stale data while another holds fresh data.

**Distributed caching** solves this by maintaining a shared cache that all application servers connect to. Every server reads from and writes to the same central cache cluster, ensuring a consistent view of cached data across the entire system.

---

## 3. What is Redis?

**Redis** (Remote Dictionary Server) is an open-source, in-memory data structure store. It is widely used as a cache, a message broker, and a session store. Redis stores all its data in RAM, which makes read and write operations extremely fast — typically in the range of microseconds.

Redis supports rich data structures, unlike traditional key-value stores that only support plain strings. It is single-threaded for command execution, which eliminates the need for locking and makes it predictable in behavior.

**Key characteristics of Redis:**
- In-memory storage (data lives in RAM)
- Persistence options available (data can be saved to disk)
- Support for complex data types
- Built-in replication and clustering
- Atomic operations on data structures
- Sub-millisecond latency

---

## 4. Redis Data Types

Understanding Redis data types is critical for interviews. The right data type must be chosen based on the use case.

| Data Type             | Description                                 | Common Use Case                            |
|-----------------------|---------------------------------------------|--------------------------------------------|
| **String**            | Plain text or binary data, up to 512 MB     | Session tokens, counters, simple key-value |
| **List**              | Ordered collection of strings               | Message queues, activity feeds             |
| **Set**               | Unordered collection of unique strings      | Tags, unique visitors, friend lists        |
| **Sorted Set (ZSet)** | Set with a floating-point score per element | Leaderboards, rate limiting                |
| **Hash**              | A map of field-value pairs                  | User profile objects, product details      |
| **Bitmap**            | Bit-level operations on strings             | Feature flags, daily active user tracking  |
| **HyperLogLog**       | Probabilistic cardinality estimation        | Counting unique users approximately        |
| **Stream**            | Append-only log data structure              | Event sourcing, real-time analytics        |

---

## 5. Cache Eviction Policies

When Redis runs out of memory, it needs to decide which keys to remove. This decision is governed by the **eviction policy**, configured via the `maxmemory-policy` setting.

- **noeviction** — Returns an error when memory is full. No keys are removed. Used when data loss is unacceptable.
- **allkeys-lru** — Evicts the **Least Recently Used** key from all keys. Most commonly used for general caching.
- **volatile-lru** — Evicts the Least Recently Used key, but only from keys that have an expiry (TTL) set.
- **allkeys-lfu** — Evicts the **Least Frequently Used** key from all keys. Useful when access patterns vary widely.
- **volatile-lfu** — Same as `allkeys-lfu`, but limited to keys with TTL.
- **allkeys-random** — Evicts a random key from all keys.
- **volatile-random** — Evicts a random key from those with TTL set.
- **volatile-ttl** — Evicts the key with the **shortest remaining TTL** first.

> The difference is that `volatile-lru` only considers keys with an expiry set, making it safer for scenarios where some keys must never be evicted.

---

## 6. Cache Expiry (TTL)

Every key in Redis can be assigned a **Time To Live (TTL)**, after which it is automatically deleted. TTL prevents stale data from living in the cache indefinitely.

```redis
SET user:1001 "John Doe" EX 3600    # Expires in 3600 seconds (1 hour)
EXPIRE user:1001 600                # Set TTL on an existing key
TTL user:1001                       # Check remaining TTL (-1 means no expiry, -2 means key does not exist)
PERSIST user:1001                   # Remove TTL, making the key permanent
```

Choosing an appropriate TTL is a design decision. A very short TTL reduces stale reads but increases cache misses and database load. A very long TTL reduces misses but risks serving outdated data.

---

## 7. Caching Strategies

The strategy defines how data moves between the cache and the database.

### 7.1 Cache-Aside (Lazy Loading)

This is the most common pattern. The application code is responsible for managing the cache.

**Read flow:**
1. Application checks the cache for the requested data.
2. On a cache hit, data is returned directly.
3. On a cache miss, the application fetches data from the database, stores it in the cache, then returns it.

**Write flow:**
- The application writes directly to the database.
- The corresponding cache entry is either deleted (invalidation) or updated.

**Pros:** Only requested data is cached; the cache is not polluted with data that is never read.  
**Cons:** The first request after a cache miss always goes to the database (cold start problem). There is a potential for stale reads between the time the database is updated and the cache is invalidated.

---

### 7.2 Write-Through

Every write to the database is also written to the cache simultaneously. The cache is always kept in sync with the database.

**Pros:** Cache is always fresh; no stale reads.  
**Cons:** Every write operation is slower because two writes happen (cache + database). The cache may be filled with data that is written but never read.

---

### 7.3 Write-Behind (Write-Back)

The application writes data to the cache first. The cache then asynchronously flushes the data to the database after a short delay.

**Pros:** Write operations are very fast since only the cache is updated immediately.  
**Cons:** Risk of data loss if the cache crashes before flushing to the database. More complex to implement correctly.

---

### 7.4 Read-Through

The cache sits in front of the database and handles all reads itself. When a cache miss occurs, the cache (not the application) is responsible for fetching data from the database and populating itself.

**Pros:** Application code is simpler — it only talks to the cache.  
**Cons:** First read is always slow. Cache warms up lazily.

---

### 7.5 Refresh-Ahead

The cache proactively refreshes data before it expires, based on predicted access patterns.

**Pros:** Reduces cache miss latency for hot data.  
**Cons:** May refresh data that is not needed, wasting resources.

---

## 8. Cache Invalidation

**Cache invalidation** is the process of removing or updating stale data in the cache when the underlying data in the database changes. It is one of the most challenging aspects of caching because it requires keeping the cache and database in sync.

**Common approaches:**

- **TTL-based expiry** — Cache entries expire automatically after a set duration. Simple but can serve stale data up to the TTL duration.
- **Event-driven invalidation** — When data changes in the database, an event is published (e.g., via a message queue). Consumers of this event delete the corresponding cache key.
- **Write-through invalidation** — The application deletes or updates the cache key at the same time it updates the database.
- **Versioned keys** — Instead of invalidating, new data is stored under a new key. Old keys naturally expire.

---

## 9. Cache Stampede (Thundering Herd Problem)

A **cache stampede** occurs when a popular cache key expires and many requests arrive simultaneously. All of them find a cache miss and simultaneously query the database, causing a spike in database load.

**Solutions:**

- **Locking / Mutex** — Only one request is allowed to fetch from the database and repopulate the cache. Others wait. Redis can implement this using `SET key value NX PX timeout` (SET if Not Exists with a TTL).
- **Probabilistic Early Expiration** — A key is refreshed slightly before its actual TTL expires, based on a probability function. This prevents simultaneous expiry of many requests.
- **Background refresh** — A background job proactively refreshes the cache before keys expire.
- **Stale-while-revalidate** — Serve the stale value to all requests while one background process refreshes it.

---

## 10. Redis Persistence

By default, Redis is an in-memory store — all data is lost on restart. Redis provides two persistence mechanisms to address this.

### 10.1 RDB (Redis Database Backup)
Redis takes a **point-in-time snapshot** of all data and writes it to a `.rdb` file on disk at configurable intervals.

- Fast restarts because it loads a single compact binary file.
- Data written between the last snapshot and a crash is lost.
- Best for use cases where losing a few minutes of data is acceptable.

### 10.2 AOF (Append-Only File)
Every write command is logged to a file as it happens. On restart, Redis replays all commands to reconstruct the dataset.

- Much more durable than RDB — can be configured to sync to disk after every command.
- AOF files are larger and restarts are slower compared to RDB.
- Best for use cases requiring high durability.

### 10.3 Hybrid Persistence
Redis 4.0 and later supports a hybrid mode: AOF files are prefixed with an RDB snapshot, giving fast restarts (from the RDB snapshot) with high durability (from the AOF log written after the snapshot).

> **Interview Tip:** For caching use cases, persistence is often disabled entirely to maximize performance. For session storage or rate-limiting counters, AOF is preferred for durability.

---

## 11. Redis Replication

Redis supports **master-replica replication**. One Redis instance is the master (primary), and one or more instances are replicas (secondaries). All write commands go to the master. The master streams every write operation to all replicas asynchronously.

**Key points:**
- Replication is asynchronous by default — there may be a brief lag between a write on the master and its appearance on replicas.
- Replicas serve read requests, enabling horizontal scaling of read-heavy workloads.
- If the master fails, a replica can be manually or automatically promoted to become the new master.

---

## 12. Redis Sentinel

**Redis Sentinel** provides high availability for Redis. It is a separate process that monitors the Redis master and replica instances.

**What Sentinel does:**
- **Monitoring** — Sentinel continuously checks whether the master and replicas are alive.
- **Notification** — Sentinel notifies administrators (or other systems) when a failure is detected.
- **Automatic failover** — If the master is unreachable, Sentinel promotes one of the replicas to be the new master and reconfigures other replicas to follow the new master.
- **Configuration provider** — Clients connect to Sentinel first to discover the current master's address.

Sentinel requires at least **3 Sentinel instances** deployed on separate machines to achieve a reliable quorum-based decision (majority vote needed to declare a master as down).

---

## 13. Redis Cluster

**Redis Cluster** enables horizontal scaling by distributing data across multiple Redis nodes. It supports both sharding (partitioning data) and high availability (automatic failover).

### How data is distributed:
Redis Cluster divides the key space into **16,384 hash slots**. Each key is mapped to a hash slot using the formula:

```
slot = CRC16(key) mod 16384
```

Each master node in the cluster is responsible for a subset of these 16,384 slots.

**Example:**
- Node A: slots 0 – 5460
- Node B: slots 5461 – 10922
- Node C: slots 10923 – 16383

### Key features:
- **Automatic sharding** — Data is automatically distributed across nodes based on hash slots.
- **No single point of failure** — Each master has at least one replica. If a master fails, its replica is promoted.
- **Cluster topology** — All nodes know about all other nodes. If a client connects to the wrong node for a given key, that node responds with a `MOVED` redirect pointing to the correct node.
- **Minimum setup** — Requires at least 3 master nodes (6 nodes total if each master has one replica).

> Redis Sentinel is for **high availability** of a single Redis master. Redis Cluster is for **horizontal scaling and partitioning** data across multiple nodes. Both provide failover but serve different purposes.

---

## 14. Redis Cluster vs. Sentinel — Comparison

| Feature              | Redis Sentinel                    | Redis Cluster                                |
|----------------------|-----------------------------------|----------------------------------------------|
| Purpose              | High availability                 | Scaling + HA                                 |
| Data partitioning    | No (single dataset)               | Yes (hash slots)                             |
| Horizontal scaling   | No                                | Yes                                          |
| Automatic failover   | Yes                               | Yes                                          |
| Minimum nodes needed | 3 Sentinel + 1 master + 1 replica | 6 (3 masters + 3 replicas)                   |
| Client complexity    | Low (clients talk to Sentinel)    | Medium (clients must handle MOVED redirects) |

---

## 15. Distributed Locking with Redis

In distributed systems, when multiple processes need to access a shared resource, a **distributed lock** prevents race conditions. Redis is commonly used to implement distributed locks.

### Basic lock using SET NX (Set if Not Exists):
```redis
SET lock:resource "unique-value" NX PX 30000
# NX = only set if key does not exist
# PX 30000 = expire in 30,000 milliseconds (30 seconds)
```

The lock is acquired if the SET command succeeds. The `NX` ensures only one client can hold the lock. The `PX` ensures the lock is automatically released even if the client crashes (preventing deadlocks).

### Releasing the lock:
The lock must be released only by the client that acquired it. This is done using a Lua script to ensure atomicity:

```lua
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
```

Without this check, a slow client might accidentally release a lock held by a different client.

---

## 16. Rate Limiting with Redis

Redis is a natural fit for implementing **rate limiting** because of its atomic increment operations and TTL support.

### Fixed Window Counter:
```redis
INCR rate:user:1001:2024010115   # Increment counter for this user in this time window
EXPIRE rate:user:1001:2024010115 3600  # Set window duration
```

If the counter exceeds the allowed limit, the request is rejected.

### Sliding Window with Sorted Sets:
For a more accurate sliding window, each request timestamp is stored in a sorted set. To check the rate, all entries older than the window duration are removed and the remaining count is checked.

```redis
ZADD rate:user:1001 <timestamp> <unique-request-id>
ZREMRANGEBYSCORE rate:user:1001 0 <timestamp - window>
ZCARD rate:user:1001
```

---

## 17. Session Management with Redis

Web applications commonly store HTTP session data in Redis. When a user logs in, a session token is generated and the session data (user ID, roles, preferences) is stored in Redis under that token as the key.

```redis
HSET session:abc123 userId 1001 role "admin" createdAt "2024-01-01"
EXPIRE session:abc123 86400   # Session valid for 24 hours
```

On each request, the server reads the session from Redis using the token from the cookie. This approach works seamlessly across multiple application servers since all of them share the same Redis instance.

---

## 18. Questions & Answers

**Q: What is the difference between Redis and Memcached?**  
- Redis supports complex data structures (lists, hashes, sorted sets), persistence, replication, and Lua scripting. 
- Memcached supports only plain strings and offers no persistence. 
- Redis is generally preferred unless an extremely simple, volatile key-value cache is needed.

---

**Q: How does Redis handle concurrent writes?**  
- Redis processes all commands in a single-threaded event loop. This means commands are executed one at a time, which eliminates race conditions within Redis itself. 
- For atomic multi-step operations, **transactions** (`MULTI/EXEC`) or **Lua scripts** are used.

---

**Q: What is a hot key problem in Redis?**  
- A hot key is a cache key that receives a disproportionately large number of requests.
- For example, the data for a trending product. All traffic for that key goes to a single Redis node, potentially overloading it. 
- Solutions include **key replication** (storing the same data under multiple keys and randomly selecting one), **local caching** (caching the hot key at the application server level), or **read replicas** (directing reads to replicas).

---

**Q: What is cache penetration?**  
- Cache penetration occurs when requests are made for data that does not exist in either the cache or the database. 
- Because there is no data to cache, every request reaches the database, defeating the purpose of caching. 
- The solution is to cache a **null or empty result** for non-existent keys with a short TTL, or to use a **Bloom filter** to quickly determine whether a key can possibly exist before querying the database.

---

**Q: What is cache avalanche?**  
- Cache avalanche occurs when a large number of cache keys expire at the same time (or the entire cache goes down), causing a massive flood of requests to hit the database simultaneously. 
- Solutions include **staggering TTLs** (adding a small random offset to expiry times), **circuit breakers** (limiting requests to the database under high load), and **cache warming** (pre-populating the cache before it goes live).

---

**Q: Can Redis lose data?**  
- With default configuration (no persistence), Redis can lose all data on restart. 
- With RDB snapshots, data written between the last snapshot and a crash is lost. 
- With AOF and `fsync` set to `always`, data loss is minimized to at most one command. 
- In practice, caches are often deployed without persistence because the database is the source of truth.

---

## 20. Best Practices

- **Always set a TTL on cached data.** Unbounded cache growth can exhaust memory and trigger unexpected evictions.
- **Monitor memory usage.** Set a `maxmemory` limit and choose an appropriate eviction policy to prevent Redis from consuming all available RAM.
- **Use connection pooling.** Creating a new connection for every operation is expensive. Connection pools (e.g., via Jedis or Lettuce in Java) reuse existing connections.
- **Size the cache appropriately.** The cache should hold the hot dataset — typically the top 20% of data that handles 80% of the traffic (Pareto principle).
- **Do not store large objects.** Storing megabyte-sized values in Redis consumes memory rapidly and can degrade performance. Break large objects into smaller pieces or consider whether they truly need to be cached.

---
