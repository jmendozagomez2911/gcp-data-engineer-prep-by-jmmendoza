# 🔌 Module — Dataflow Sources & Sinks (Beam I/O): Text/File, BigQuery, Pub/Sub, Kafka, Bigtable, Avro, Splittable DoFn + Quiz

At this point in the streaming journey, you already know **what Dataflow/Beam does (processing + event time)**. This module focuses on **how data gets in and out** reliably and efficiently.

In Beam terms:

* **Source** = read external data into a pipeline (creates a `PCollection`)
* **Sink** = write from a pipeline to an external system (often returns `PDone`)

This is exam-relevant because most “pick the tool / pick the method” questions are really **I/O questions**:

* *How do you read it efficiently?*
* *How do you write it reliably?*
* *How do you handle streaming vs batch differences?*
* *Where do duplicates/late data/ack semantics bite you?*

For the evolving list of connectors, Beam keeps the canonical list. ([beam.apache.org][1])

---

## 1) 🧩 Why this part exists in the pipeline

A pipeline is only as good as its boundaries:

### What Sources solve

* **Ingestion**: connect to systems (files, BigQuery, Pub/Sub, Kafka, Bigtable, etc.)
* **Parallelism**: split reading work so it scales
* **Correctness signals** (streaming): checkpoints + watermarks + (sometimes) dedup hints

### What Sinks solve

* **Durable output** to storage/serving/analytics systems
* **Delivery semantics** trade-offs (latency vs cost vs throughput)
* **Operational reliability**: retries, dead-letter patterns, “what happens when writes fail?”

---

## 2) 🧠 Core concepts (first principles)

### ✅ Source vs Sink vs PTransform vs PDone

* A **PTransform** is an operation: it takes input and produces output.
* A **sink is a PTransform** that writes to an external destination.
* Many sinks return **`PDone`**, meaning “this branch is finished” (no more elements to process after it).
* Some sinks (or custom composites) can be designed to **produce output** (e.g., results, failed writes, signals), but the default mental model is: **write → done**.

### ✅ Sources don’t have to be at the beginning

Typical pipeline: `Read → Transform → Write`.

But you can also have:

* `Read topic → parse filename → Read file → Transform → Write`
  So the *second* read is still a **source**, just not the first step. This matters for “stream of files” patterns.

---

## 3) 🧱 Bounded vs Unbounded: the one distinction you must not blur

### 📦 Bounded source (batch shape)

**Bounded = finite input** (known start/end).

* Beam/Dataflow can:

    * split work into **bundles**,
    * estimate bytes,
    * **dynamically rebalance** if it can split further (helps load-balance stragglers).

**Real-life implication:** bounded reads are easier to parallelise because the system can plan around “how much is left”.

### 🌊 Unbounded source (streaming shape)

**Unbounded = potentially infinite input**.
Key mechanisms:

* **Checkpoints**: bookmark progress so you don’t reread from the beginning after failures.
* **Watermarks**: “how complete we think event-time is” signals from the source.
* Optional **record/message IDs** to support dedup.

**Exam nuance:** unbounded ≠ “fast”. Unbounded means **no end**, which makes correctness about *time* and *state*.

---

## 4) ⏱️ Streaming-specific source signals: checkpoints, watermarks, dedup IDs

### ✅ Checkpoints (bookmarking)

Used by unbounded sources (e.g., Kafka offsets) to resume where you left off after retries/restarts.

### ✅ Watermarks (event-time progress hints)

Sources can provide watermark estimates so windowing/triggers can decide when to emit “on-time” vs “late”.

### ✅ Record IDs for dedup (the 10-minute trap)

Pub/Sub + Dataflow can use an **ID attribute** (record ID label) to deduplicate messages **only within a limited time horizon**:

* duplicates must arrive within **~10 minutes** for the dedup guarantee to hold. ([Google Cloud Documentation][2])

**What breaks in real life:**

* If duplicates can occur **hours later** (replays, retries, upstream bugs), this built-in dedup is not enough.
* You need application-level dedup (stateful keys, BigQuery constraints patterns, idempotent writes, etc.).

---

## 5) 📄 TextIO & FileIO: “text lines” vs “files as objects”

### 🧾 TextIO (simple: lines in / lines out)

Use when:

* You’re dealing with text-based data where “a line = a record”
* You want straightforward read/write semantics

### 📦 FileIO (file-aware: metadata + matching + watching)

FileIO is about treating files as **objects**:

* **Match by pattern** (glob/filepattern)
* Access **filename + metadata**
* Useful for ranges of files (partitioned exports, daily drops, sharded outputs)

#### 🔁 “Stream of files” pattern (important)

Two common ways to process files that arrive continuously:

1. **Watch a location** (polling):

* FileIO can **monitor** a location for matching files on an interval (e.g., every 30s for 1 hour).
* Good when files arrive within a known arrival window and you can tolerate polling.

2. **Event-driven file ingestion** (Pub/Sub notifications):

* Cloud Storage can publish metadata change notifications to Pub/Sub.
* Pipeline flow:

    * **PubsubIO read message**
    * parse message → extract filename
    * then **FileIO/TextIO read file**
* This is the classic “file stream” model: *messages drive file reads*.

**Gotcha (ops):** file-notification systems can produce duplicates or out-of-order notifications → you may need dedup by filename+generation or event id.

### 🧠 Contextual I/O: “TextIO, but smarter”

Historically, when TextIO wasn’t enough (multi-line records, positions, richer parsing), you’d be pushed to FileIO.

Contextual I/O extends TextIO so you can:

* read multi-line CSV-like records
* get extra context like ordinal position

**Exam cue:** if the problem is “still text, but needs richer parsing context”, contextual TextIO is the intended answer.

---

## 6) 🎯 Dynamic destinations (file sinks): runtime routing without code changes

Dynamic destinations = decide the output target **at runtime** based on data attributes:

* Route by `transaction_type` → output filename/table/topic depends on record.
* In Python examples, they push further: route to **different sink types** depending on record type.

**Why this matters:**

* You can expand output targets without redeploying different pipelines.
* In exams, dynamic destinations is the “multi-tenant / multi-table / multi-file outputs” pattern.

**Real-life gotcha:**

* Too many destinations can create:

    * excessive small files (GCS), or
    * too many tables/partitions (BigQuery quotas / management burden).
      So dynamic routing is powerful, but you still need bounded cardinality.

---

## 7) 🧮 BigQueryIO: reading and writing without accidentally burning time/cost

BigQueryIO is *not* one thing; it’s multiple strategies.

### 🟦 Reading from BigQuery: two distinct paths

#### Path A) Export-based read (common when reading query results)

When you read using a SQL query, Dataflow:

1. submits query
2. retrieves metadata
3. BigQuery **exports results to a temporary Cloud Storage location**
4. Dataflow reads the exported files for throughput ([Google Cloud Documentation][3])

**Why this exists:** BigQuery is an analytics engine; exporting to files lets Dataflow read in parallel like a file-based batch.

**Gotcha:** you need:

* a **GCS temp/staging location**,
* correct IAM to write/read that temp export,
* and you may see “why is it writing to GCS?” confusion (this is expected). ([Google Cloud Documentation][3])

#### Path B) Direct read via BigQuery Storage Read API (`DIRECT_READ`)

This is the high-throughput distributed-read option:

* Uses the BigQuery Storage Read API (no export step)
* Enabled via **`DIRECT_READ`** ([Google Cloud Documentation][3])

Big benefits:

* faster than export in many cases
* built for distributed frameworks like Beam ([beam.apache.org][4])

##### Cost/perf controls you should remember

* **Column projection**: `withSelectedFields` to read fewer columns (scan less).
* **Row restriction**: `withRowRestriction` to filter rows early. ([beam.apache.org][5])

**Exam trigger:**

* If you see “reduce scan / read only needed columns / predicate pushdown” → mention `withSelectedFields` + `withRowRestriction` (and that it’s tied to Storage API / direct reads). ([beam.apache.org][5])

---

### 🟩 Writing to BigQuery: streaming vs file loads (and the windowing trick)

BigQueryIO can write in:

* **Streaming writes** (default for streaming jobs)
* **File loads** (`FILE_LOADS`) when you micro-batch using windowing

Key idea from the module:

> A streaming pipeline can write “in batches” by windowing + `FILE_LOADS`.

**Decision rule (high-yield):**

* Need **lowest latency** (seconds) → streaming write path (default).
* Need **better throughput/cost** and can tolerate windowed delay → window + `FILE_LOADS`.

**Real-life gotchas:**

* `FILE_LOADS` means:

    * temporary files in GCS,
    * load job constraints,
    * more moving parts to permission correctly.
* Streaming writes can create:

    * higher cost per GiB,
    * and you must still design for duplicates (streaming ≠ magically exactly-once end-to-end).

### 🎯 Dynamic destinations into BigQuery (multi-table + varying schemas)

Dynamic destinations can route to:

* different tables,
* potentially with different schemas.

This is extremely powerful, but **quota and governance heavy** if misused.

---

## 8) 📬 PubsubIO: the “front door” for streaming (with important semantics)

### Reading patterns: topic vs subscription (lifecycle difference)

* If you read from a **topic** without specifying a subscription, Beam/Dataflow can create a **temporary subscription** for you. ([beam.apache.org][6])
* If you need the subscription to **persist** (seek/replay/control), create it and read via **fromSubscription**.

**Exam trigger:**

* “Subscription must remain after job ends” → use an existing subscription (not ephemeral topic-read). ([beam.apache.org][6])

### Ack semantics: “acked when durably persisted” (subtle but crucial)

The module states: Dataflow acknowledges Pub/Sub messages when they are **durably persisted / materialised** inside Dataflow (not when your final sink has succeeded).

**Why this matters (what breaks):**

* If your pipeline later fails after ack, Pub/Sub will not necessarily redeliver those messages.
* So you must design downstream to be:

    * idempotent, or
    * have failure capture paths (DLQ), or
    * checkpoint state properly.

This is one reason DLQs are emphasized: you need an explicit plan for “bad events”.

### Timestamps: published timestamp vs event timestamp

By default, Pub/Sub message timestamps are used for windowing.
But you can **reassign** timestamps:

* Use reported/source timestamp inside payload for event-time correctness (recommended if publish time ≠ event time).

**Exam trigger:**

* “Event time is in the payload” → parse + assign event-time timestamp; otherwise windows are wrong.

### Dead-letter queue (DLQ)

DLQ = “divert messages that meet criteria”:

* parsing failures
* schema mismatch
* invalid business rules
* suspicious events

**Exam cue:**

* “Capture failures for later inspection” → DLQ topic/table + monitoring.

---

## 9) 🧩 KafkaIO: unbounded reads + checkpoints + cross-language

KafkaIO in Beam:

* is an **unbounded source** (streaming)
* uses **checkpoints** (Kafka offsets) so it can resume correctly
* subscribes to topics via:

    * `withTopics([...])` (multiple topics)
    * `withTopic(...)` (single topic)

### Cross-language transforms (why Python can use KafkaIO)

KafkaIO is built in Java, but Beam supports **multi-language pipelines**:

* Python KafkaIO can call into the Java implementation via cross-language transforms.

**Real-life gotcha:**

* Cross-language introduces:

    * extra runtime components (expansion services / external transforms),
    * version alignment issues between SDKs.
      Not always asked directly on exams, but it explains why some connectors “exist” in Python even if implemented in Java.

---

## 10) 🗄️ BigtableIO: streaming-friendly serving sink (and the “continue after write” pattern)

Bigtable is Google’s high-throughput NoSQL service, and BigtableIO is designed to match that throughput.

### Reading optimisations you should remember

* **Row filters** (`withRowFilter`) to avoid scanning everything
* **Key range / prefix scan** (`withKeyRange`) to leverage sorted row keys and scan by prefix

**Exam trigger:**

* “Need fast lookup / serving state / high QPS reads” → Bigtable, not BigQuery.

**Real-life gotcha:**

* Bigtable performance depends on **row key design**:

    * naive prefixes can hotspot (everyone writes to same key region).
      Prefix scans are powerful, but poorly distributed keys cause pain.

### Continue processing after a sink: `Wait.on`

This module highlights a special case:

* Some write patterns allow you to **gate** later steps until the write completes.
* In Beam, `Wait.on` can be used with BigtableIO by emitting a signal and waiting on it.

**Why it exists:** sometimes you need a “write finished” barrier before triggering a dependent branch.

**Gotcha (ops):**

* If the signal never fires (errors, filtering, conditional branches), your pipeline can appear “stuck”.

---

## 11) 🧾 AvroIO: self-describing files (schema + data together)

Avro:

* stores **schema + data** in the file → “self-describing”
* common in big data pipelines

AvroIO supports:

* reading and writing Avro
* extracting schema into the pipeline
* wildcard reads for multiple files

**Exam trigger:**

* “Need schema embedded in file / strong schema evolution story” → Avro is the canonical answer (vs raw CSV).

**Real-life gotcha:**

* Schema evolution still needs discipline (compatibility rules), or downstream consumers break.

---

## 12) 🧠 Splittable DoFn (SDF): build custom sources like a “source-powered DoFn”

SDF = **DoFn with source-like powers**:

* **splittability** (divide work into smaller restrictions/bundles)
* **progress reporting** (how far along a restriction is)
* more seamless unified batch + streaming model

### Mental model

Think of an SDF as:

* a normal `DoFn`
* plus a **Restriction** (what work exists)
* plus a **RestrictionTracker** (how much is done, can we split?)

Example concept from the transcript:

* reading a file can be expressed as restrictions like “byte ranges / blocks”
* Dataflow can then split those ranges across workers for parallelism.

### Why it exists (practical)

Use SDF when:

* there is **no ready-made connector**,
* or you need a custom source pattern (APIs, paginated reads, bespoke file formats),
* and you still want Dataflow’s dynamic splitting + observability.

Beam provides guidance and examples for building custom I/O when existing connectors don’t fit. ([beam.apache.org][7])

**Exam cue:**

* “Need to implement a custom source that can split and report progress” → Splittable DoFn.

**Gotcha (real life):**

* If you implement restriction sizing poorly:

    * you get bad parallelism (too large restrictions),
    * or overhead explosion (too many tiny restrictions),
    * or incorrect checkpointing.

---

## 13) 🧪 Exam cheats: decision rules + common traps

### 🔥 High-yield decision rules

1. **Finite dataset** → bounded source → batch patterns
2. **Infinite stream** → unbounded source → checkpoints + watermarks
3. **Need to read many files by pattern** → FileIO (match pattern)
4. **Need a stream of files arriving** → FileIO watch *or* Pub/Sub notifications → file read
5. **BigQuery read for high throughput** → `DIRECT_READ` (Storage Read API) ([Google Cloud Documentation][3])
6. **Reduce BigQuery scan** → `withSelectedFields` + `withRowRestriction` (direct reads) ([beam.apache.org][5])
7. **Pub/Sub subscription must persist** → read from existing subscription (not temporary) ([beam.apache.org][6])
8. **Dedup on Pub/Sub message IDs** → only reliable within ~10 minutes → otherwise do your own dedup ([Google Cloud Documentation][2])
9. **Serving / low-latency lookups** → Bigtable, not BigQuery
10. **No connector exists + need splitting/progress** → Splittable DoFn (custom source)

### 🧨 Common traps

* “Pub/Sub dedup = exactly once forever” ❌ (10-minute window) ([Google Cloud Documentation][2])
* “Ack means the final write succeeded” ❌ (acked when materialised in Dataflow, not necessarily when sink is complete)
* “BigQuery read always reads from BigQuery directly” ❌ (export-to-GCS path exists; direct read is explicit) ([Google Cloud Documentation][3])
* “Dynamic destinations are always good” ❌ (unbounded destinations cause operational quota + small-file problems)

---

# ✅ Quiz 3 — Sources & Sinks (integrated)

## Q1) What kinds of data are a bounded and an unbounded source respectively associated with?

✅ **Correct:** **Batch data and streaming data.**

**Why:**

* **Bounded** sources read a finite input → batch-shaped workloads.
* **Unbounded** sources read an infinite input → streaming-shaped workloads with checkpoints/watermarks.

Why the other options are wrong:

* “Time-series vs graph”, “small vs big”, “structured vs unstructured” describe *data types*, not the bounded/unbounded execution model.

---

## Q2) What is the simplest form of a sink?

✅ **Correct:** **PTransform**

**Why:**

* In Beam, a **sink is a PTransform** that writes to an external destination and commonly returns `PDone` to mark the branch complete.

Why the others are wrong:

* **PSink** is not a core Beam abstraction here.
* **PCollection** is data, not an operation.
* “Built-in primitive function” isn’t the Beam model; Beam composes transforms (`PTransform`) rather than calling primitive sink functions.

[1]: https://beam.apache.org/documentation/io/connectors/?utm_source=chatgpt.com "I/O Connectors - Apache Beam®"
[2]: https://docs.cloud.google.com/dataflow/docs/concepts/streaming-with-cloud-pubsub?utm_source=chatgpt.com "Read from Pub/Sub to Dataflow"
[3]: https://docs.cloud.google.com/dataflow/docs/guides/read-from-bigquery?utm_source=chatgpt.com "Read from BigQuery to Dataflow"
[4]: https://beam.apache.org/documentation/io/built-in/google-bigquery/?utm_source=chatgpt.com "Google BigQuery I/O connector - Apache Beam®"
[5]: https://beam.apache.org/releases/javadoc/2.17.0/org/apache/beam/sdk/io/gcp/bigquery/BigQueryIO.TypedRead.html?utm_source=chatgpt.com "BigQueryIO.TypedRead (Apache Beam 2.17.0)"
[6]: https://beam.apache.org/releases/pydoc/2.36.0/apache_beam.io.gcp.pubsub.html?utm_source=chatgpt.com "apache_beam.io.gcp.pubsub module"
[7]: https://beam.apache.org/documentation/io/developing-io-overview/?utm_source=chatgpt.com "Overview: Developing a new I/O connector - Apache Beam®"
