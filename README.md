# ConcurrentHashMap

---

## What is the Problem First?

In real Java applications, many threads are running at the same time and they often need to read/write from a shared `Map`.

A normal `HashMap` is not built for this kind of situation at all. If two threads modify it at the same time, it can silently corrupt the data or even go into an infinite loop. Very dangerous.

---

## Why Not Use Old Alternatives?

| Map Type                      | Thread-Safe? | How it Locks          | Problem                               |
|-------------------------------|--------------|-----------------------|---------------------------------------|
| `HashMap`                     | No           | No lock at all        | Data corruption, infinite loops       |
| `Hashtable`                   | Yes          | Locks entire map      | Only one thread at a time — very slow |
| `Collections.synchronizedMap` | Yes          | Locks entire map      | Same bottleneck as `Hashtable`        |
| `ConcurrentHashMap`           | Yes          | Locks only one bucket | High throughput, much better          |

`Hashtable` and `Collections.synchronizedMap` both put one big lock on the whole map. So even if 100 threads want to read different keys, they all have to wait for each other one by one. This becomes a serious bottleneck when load is high.

`ConcurrentHashMap` is smarter — it locks only the small part of the map that is actually being touched. Rest of the map is free for other threads.

---

## How It Works Internally

### Java 7 — Segment Based Locking

In Java 7, the map was divided into 16 **segments** by default. Each segment was like a small independent `HashMap` with its own `ReentrantLock`. Only the segment being written to was locked. Other segments were fully accessible.

This was much better than global locking, but the fixed 16-segment limit still put a cap on concurrency.

### Java 8 Onwards — CAS + Bucket Level Locking (Current Design)

Java 8 completely redesigned it. Segments are gone. Now the internal structure is just an **array of buckets**, similar to normal `HashMap`.

Each bucket holds either a linked list or a balanced tree (when chain length goes beyond 8 nodes). Thread safety comes from two different mechanisms depending on what state the bucket is in.

```
Bucket 0:  [A → 1] → [D → 4]   ← synchronized on this head node only
Bucket 1:  [B → 2]
Bucket 2:  [C → 3] → [E → 5]   ← separate lock, totally independent
Bucket 3:  (empty)              ← CAS is used, no lock at all
```

#### CAS (Compare-And-Swap) — When Bucket is Empty

When a thread inserts into an **empty bucket**, no lock is taken at all. Instead, a **CAS operation** is used. This is a hardware-level atomic instruction that checks if the bucket is still empty and places the new node in one shot. If another thread already wrote there in the meanwhile, CAS detects it and retries. Since CAS does not involve OS scheduler, it is much faster than acquiring a lock.

#### `synchronized` on Bucket Head — When Bucket Already Has Nodes

When a key maps to a bucket that already has some nodes (collision case), the thread takes a `synchronized` lock on **that bucket's head node only**. All other buckets are completely untouched. Two threads writing to different buckets run fully in parallel. Only threads writing to the exact same bucket have to wait for each other.

In a large map with many buckets, the chance of two threads hitting the same bucket is very low. So effectively thousands of threads can work simultaneously without blocking each other.

---

## Important Characteristics

### Reads are Near Lock-Free

Read operations do not acquire any lock under normal circumstances. Node values are declared `volatile`, so any write done by one thread is immediately visible to all other threads without any synchronisation needed. This makes `ConcurrentHashMap` very fast for read-heavy workloads.

### null Keys and Values are Not Allowed

`ConcurrentHashMap` does not allow `null` as key or value. This is by design. In a concurrent setting, if `get(key)` returns `null`, it is ambiguous — it could mean key is absent, or value is genuinely `null`. To avoid this confusion, `null` is simply banned.

### Atomic Compound Operations

Normal `HashMap` has no atomic compound operations. Things like "insert only if not present" need external synchronisation. `ConcurrentHashMap` provides these out of the box:

| Method                         | What it does                                     |
|--------------------------------|--------------------------------------------------|
| `putIfAbsent(key, value)`      | Inserts only if key is not already there         |
| `computeIfAbsent(key, fn)`     | Computes and inserts value only if key is absent |
| `compute(key, fn)`             | Atomically updates value based on current state  |
| `merge(key, value, fn)`        | Atomically merges new value with existing one    |
| `replace(key, oldVal, newVal)` | Replaces only if current value matches `oldVal`  |

All these methods do the check and the update as one single atomic operation at bucket level. No external `synchronized` block is needed.

---

## When to Use It

| Use it when...                                    | Do not use it when...                                     |
|---------------------------------------------------|-----------------------------------------------------------|
| Multiple threads are reading/writing the same map | You need a consistent snapshot of full map at one instant |
| Shared cache with concurrent lazy loading         | `null` values have some meaning in your design            |
| Frequency counters in parallel pipelines          | Code is single-threaded — plain `HashMap` is faster       |
| Shared state coordination across thread pools     | Atomicity is needed across multiple maps together         |

---

## Why ConcurrentHashMap is a Good Choice for Caching

| Property                               | Benefit                                                                 |
|----------------------------------------|-------------------------------------------------------------------------|
| Bucket-level locking via CAS (Java 8+) | High read concurrency, near lock-free reads                             |
| `computeIfAbsent()` is atomic          | Prevents duplicate computation for same key (cache stampede prevention) |
| `null` not allowed                     | Forces explicit, unambiguous handling of missing entries                |
| No global lock on reads                | Throughput scales as number of threads increases                        |

---