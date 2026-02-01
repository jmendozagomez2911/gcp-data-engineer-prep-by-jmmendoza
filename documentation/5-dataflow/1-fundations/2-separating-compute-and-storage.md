# 🧠 Module — Dataflow Premium Backends: Separate Compute & Storage (Shuffle Service, Streaming Engine, FlexRS)

This module is about one thing: **why Dataflow can run large pipelines efficiently without “throwing bigger VMs at the problem”**.

The mechanism is: **move heavy system work (shuffle + state storage + scheduling tricks) out of worker VMs** and into **managed backends**. That’s what “separating compute and storage” means here.

---

## 1) 🎯 Why this part exists in the pipeline

In real streaming/batch pipelines, your expensive pain points are not usually your `Map()` or `Filter()` logic. They are:

* **Shuffle-heavy steps** (grouping, joins, combines)
* **Streaming state** (window state, timers, triggers, per-key aggregation state)
* **Cost sensitivity** for batch jobs that aren’t time-critical

If you keep shuffle and state **on worker VMs**, you pay for:

* large machine types,
* lots of persistent disk,
* higher CPU/memory pressure,
* worse autoscaling (workers can’t scale down because they “hold” shuffle/state data)

So Dataflow introduces premium backends that:

* **offload shuffle/state from VMs**, and
* let workers focus on **user code execution**.

**Mental model:**
Workers should run *your code*. Backends should run *Dataflow’s system responsibilities*.

---

## 2) 🧠 Core concepts: what “compute vs storage separation” means in Dataflow

### What is “compute” in this context?

* Worker VMs running the **SDK harness** executing your transforms (DoFns, user code).

### What is “storage” in this context?

* Intermediate data that must be persisted for correctness and fault tolerance:

    * **shuffle data** (batch GroupByKey/joins)
    * **streaming state** (window state + per-key accumulators + timers)

### Why separation matters

If shuffle/state is on workers:

* autoscaling is constrained (scale down risks losing shuffle/state)
* failures are worse (a VM dying can kill the job if it holds critical intermediate data)
* you need bigger VMs/disks just to host system data

If shuffle/state is in a managed service:

* workers can be **smaller and stateless-ish**
* autoscaling is more aggressive and safe
* failure recovery is better

---

## 3) ⚙️ Dataflow execution optimisations (why it “feels faster”)

The transcript highlights three runtime behaviours you should be able to explain:

### ✅ Graph optimisation via fusion + pipelining

Dataflow:

* **fuses operations** efficiently (reduces overhead between steps)
* doesn’t wait for previous steps to finish before starting the next **unless a dependency forces a barrier**

**Exam interpretation:**
If there’s no shuffle/state barrier, Dataflow can pipeline execution and keep throughput high.

### ✅ Step-by-step autoscaling inside the pipeline

Autoscaling can happen **in the middle of a job**, step by step:

* resources increase when demand increases
* scale down when demand drops
* you don’t pay for idle VMs

### ✅ Strong streaming semantics maintained

Even while autoscaling and rebalancing:

* aggregations (sum/count) remain correct **even with duplicates**
* late data can be handled via **watermarks** (intelligent watermarking)

**Subtle point (exam bait):**
“Correct even with duplicates” does not mean Dataflow magically makes the source exactly-once. It means the system is designed so pipeline correctness can hold under retries/reprocessing (i.e., the engine can preserve consistent results given Beam semantics), but your sinks and side effects still matter.

---

## 4) 🔀 Dataflow Shuffle Service (Batch): offload shuffle out of worker VMs

### What is a shuffle in Beam/Dataflow?

A **shuffle** is the behind-the-scenes operation needed for transforms like:

* `GroupByKey`
* `CoGroupByKey`
* `Combine`

It:

* partitions data by key
* groups values for each key
* must be scalable, efficient, and fault tolerant

### The “before” (default shuffle)

Historically, shuffle runs **entirely on worker VMs**:

* uses worker CPU/memory
* uses persistent disk attached to workers
* creates scaling and failure constraints

### The “after” (Shuffle Service for batch)

**Shuffle Service** (batch only) moves shuffle to a **Dataflow service backend**.

#### Benefits (high-yield)

* **faster batch pipeline execution** for most job types
* **reduces VM CPU/memory/disk usage**
* **better autoscaling**

    * because workers no longer “hold” shuffle data
    * can scale down earlier
* **better fault tolerance**

    * unhealthy VM holding shuffle data won’t kill the job
    * because shuffle data is no longer pinned to that VM

### Decision rule (exam trigger)

* If you see: “batch pipeline slow / expensive due to GroupByKey/joins” → enable **Shuffle Service**.
* If you see: “autoscaling can’t scale down” and the job is batch with heavy shuffle → Shuffle Service is a prime lever.

---

## 5) 🧠 Dataflow Streaming Engine: offload streaming state + streaming shuffle

Streaming has a different killer cost than batch shuffle: **state**.

### What Streaming Engine offloads

* **window state storage** moves from worker persistent disks to a backend service
* also provides efficient shuffle for streaming cases

### Key operational point

✅ **No code changes required.**
Workers keep running your transforms and talk to Streaming Engine transparently to access state.

### Benefits (high-yield)

* reduced worker CPU/memory/persistent disk use
* works best with **smaller machine types** (example given: `n1-standard-2`)
* doesn’t need large persistent disks (beyond small boot disk)
* **lower quota consumption** (smaller VMs + less disk)
* more responsive to incoming data variation (autoscaling responsiveness improves)
* improved supportability: service updates don’t require pipeline redeploys

### Decision rule (exam trigger)

* If you see “streaming pipeline, windowing/state heavy, expensive workers/disks” → **Streaming Engine**.
* If you see “need faster responsiveness to traffic spikes” → **Streaming Engine** is a key answer.

### Gotchas / what breaks in real life

* Streaming Engine changes the resource profile: if you keep large disks/machines “just in case”, you miss its cost advantage.
* If quotas are tight (CPU/disk), Streaming Engine is a lever because it reduces disk needs and allows smaller machine types.

---

## 6) 🕒 FlexRS (Flexible Resource Scheduling): cheaper batch via delayed scheduling + mixed VMs

FlexRS is about **cost optimisation**, not speed.

### What FlexRS is (first principles)

FlexRS reduces batch cost by using:

* advanced scheduling techniques (works with Shuffle Service)
* a mix of **preemptible and normal VMs**

### The key trade-off: time flexibility

When you submit a FlexRS job:

* Dataflow queues it
* executes it **within 6 hours** from creation

So it’s suitable when:

* workload is **not time-critical**
* jobs can run within a window (daily/weekly jobs)

### Early validation (important operational feature)

Immediately upon submission, Dataflow:

* records a job ID
* performs early validation of:

    * execution parameters
    * configuration
    * quotas
    * permissions

If something is wrong, you find out **immediately**, not 5 hours later.

### Decision rule (exam trigger)

* If the question says: “reduce batch cost” + “can tolerate delayed start” → **FlexRS**.
* If it says: “time-critical / must start now” → **do not use FlexRS**.

---

# ✅ Quiz (integrated reasoning)

## Q1) True statements about FlexRS (Select ALL that apply)

Options:

* FlexRS helps to reduce batch processing costs by using advanced scheduling techniques.
* FlexRS leverages a mix of preemptible and normal VMs.
* FlexRS is most suitable for workloads that are time-critical.
* When you submit a FlexRS job, Dataflow queues it and executes within 6 hours.

✅ Correct:

* **FlexRS helps reduce batch costs by using advanced scheduling techniques.**
* **FlexRS leverages a mix of preemptible and normal VMs.**
* **FlexRS queues job and runs within 6 hours.**

❌ Incorrect:

* **“FlexRS is most suitable for workloads that are time-critical.”**
  Opposite. FlexRS is explicitly for workloads that can tolerate delay.

---

## Q2) Dataflow Shuffle Service is available only for batch jobs

✅ **True**

Reason:

* The transcript explicitly says the service-based Shuffle feature is **available for batch pipelines only**.
* Streaming has a different offload mechanism: **Streaming Engine**.

---

## Q3) Benefits of Dataflow Streaming Engine (Select ALL that apply)

Options:

* More responsive autoscaling for incoming data variations
* Reduced consumption of worker CPU, memory, and storage
* Lower resource and quota consumption

✅ **All are correct**

Why:

* Streaming Engine offloads state and streaming shuffle from worker disks → reduces worker CPU/memory/disk.
* Smaller workers + reduced disk → lower quota usage.
* State offload makes the pipeline more responsive to volume changes (autoscaling responds better).

---

# 🧾 Exam decision rules (memorise)

1. **Batch + heavy GroupByKey/joins/combine cost** → enable **Shuffle Service**
2. **Streaming + state/window-heavy, expensive workers/disks** → enable **Streaming Engine**
3. **Need cheapest batch and can wait (≤ 6h start)** → **FlexRS**
4. **Time-critical batch (must start now)** → **don’t use FlexRS**
5. **Goal = better autoscaling / scale down earlier** → offload shuffle/state to service backends

