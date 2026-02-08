## Module — State & Timers in Beam (Dataflow)

### Why this exists (first principles)

In Beam you normally aggregate with:

* **GroupByKey** (shuffle by key)
* **Combine** (keyed or global combine)

A plain **ParDo/DoFn** is *element-wise*: 0..N outputs per element, but **no “memory” across elements**.

State & Timers extend ParDo so it *can* do **stateful transformations**: keep per-key (and per-window) memory and emit results based on thresholds or time.

---

## Stateful ParDo (State API)

### Core rules

* **State is per key** → input must be **KV** (key-value).
* **In streaming**: state is also **per window** (key + window).
* State is **local to the transform** (and effectively local per key): different keys/workers don’t share a single global state.
* You can **read + mutate state inside `processElement()`**.

### Why not just use class member variables?

Because runners can retry / reprocess bundles. Dataflow provides **consistent, fault-tolerant state updates** (it won’t “double apply” mutations that weren’t fully committed).
If you used normal instance fields, you’d have to build correct “rollback / exactly-once mutation” logic yourself — messy and error-prone.

---

## Typical pattern: batching calls to an external service

Problem: calling an external API **per element** can DDoS your own dependency (millions of calls/sec).

Solution:

* Keep a **buffer state** (store elements)
* Keep a **count state**
* When count reaches N → make **one batch call**, emit results, then **clear state**

**Important gotcha**: if the stream ends (or slows), the buffer might **never reach N** → state stays forever → pipeline branch can’t “finish” cleanly, and resources get consumed.

That’s why you need timers.

---

## Timers (State + Timers together)

### What timers do

Timers are used **with state** to:

* **flush** partial buffers,
* **emit** outputs even if thresholds aren’t met,
* and, crucially, **clear state** so it doesn’t live forever.

### Two timer types

1. **Event-time timer**

* Driven by the **watermark** (progress of event time).
* Good when you want output based on **data completeness**.
* Can be delayed by late/out-of-order data and watermark behavior.

2. **Processing-time timer**

* Driven by the worker clock (wall time).
* Good for **timeouts** and predictable periodic flushing.
* Fires even if event-time progress is slow/stuck.

### Key warning

After the timer fires and you emit output: **clear the state**.
If you don’t, the DoFn may keep waiting / holding state and you waste resources.

---

## Choosing event-time vs processing-time (decision rule)

* Want **predictable latency**, willing to accept possibly incomplete results → **processing time**
* Want **complete-ish output** aligned with event-time + lateness, OK with higher latency → **event time**

---

## State types (what to use when)

* **ValueState**: one value (generic)
* **BagState**: many values, **no guaranteed order**, fast appends (good for buffers)
* **CombiningState**: best for associative/commutative aggregations (sum, count, min/max)
* **MapState**: key → value, **random access by key**, efficient lookups

Note: **SetState** exists in some contexts but (per the transcript) **not supported in Dataflow** → use **BagState** for similar behaviour.

---

## Where this is useful (examples)

* Custom per-key triggering beyond built-in windows/triggers
* Streaming “joins” patterns (holding side inputs per key)
* Slowly changing dimensions-like patterns (keep dimension state, apply updates over time)
* Any “per key workflow” needing fine control

---

# Quiz 5 — answers

### 1) What is the use case for timers in Beam’s State & Timers API?

✅ **Timers are used in combination with state variables to ensure that the state is cleared at regular time intervals.**

### 2) Can you do aggregations with ParDo?

✅ **You can do aggregations using state variables in a DoFn.**
