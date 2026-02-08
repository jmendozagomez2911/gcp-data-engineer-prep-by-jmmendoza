## Module — Dataflow Best Practices (Schemas, Errors, POJOs, DoFn lifecycle, DAG)

### What this chapter is about

After covering Beam fundamentals (windows/watermarks/triggers, I/O, schemas, state & timers), this module focuses on **how to build production-grade Dataflow pipelines**:

1. Schemas (why they help performance + maintainability)
2. Handling bad/unprocessable records (dead-letter pattern)
3. Error handling strategy (try/catch + side outputs)
4. POJO generation (AutoValue) + JSON handling (JsonToRow / Convert)
5. DoFn lifecycle for micro-batching
6. DAG (Directed Acyclic Graph) optimisation + external-system backpressure

---

# 1) Beam Schemas (best-practice angle)

### What a schema is

A schema describes a record as:

* **named fields**
* **field types** (primitive + complex)
* optional/nullable vs required
* nested / repeated structures

### Why schemas are a best practice in Dataflow

* **Readability**: code expresses business intent (“select fields”, “join”, etc.).
* **Portability**: schema is independent of any specific class shape.
* **Optimisation**: Dataflow can optimise **encoding/decoding** (serialisation/deserialisation) because it knows structure (faster movement between stages).

---

# 2) Unprocessable / erroneous records (dead-letter pattern)

### Problem

Real data is messy. If you don’t handle malformed records:

* pipelines can **fail**
* or you spam logs and still lose the data needed to debug upstream issues

### Recommended pattern

* Do *not* just log and move on.
* **Route bad records to a persistent sink** (GCS / BigQuery / Bigtable) for later analysis.

### How (Beam mechanics)

* Use **multi-output ParDo**:

    * main output = “good”
    * side output = “bad”
* Implement with **TupleTags / side outputs** (Java) or equivalent multi-output pattern (Python).

### Try/catch guidance

* Wrap user parsing/logic inside `processElement` with **try/catch**.
* **Avoid logging every exception** (can overwhelm pipeline + costs).
* Instead: emit the raw record to the dead-letter side output.

---

# 3) Error handling best practices (severity-aware)

### Guideline

Errors are normal. A fault-tolerant pipeline:

* catches exceptions in DoFn user code
* treats exceptions differently depending on severity
* persists raw problematic data, not just the message/stack trace

### Practical recommendation

Send **raw, unprocessed data** (often as a string column) to a durable store:

* BigQuery (easy querying)
* Bigtable (if you want low-latency lookups / keyed storage)

Use **TupleTags** to write to multiple sinks:

* “clean output sink”
* “dead-letter sink”

---

# 4) POJOs + AutoValue (Java)

### Why POJOs still matter

Even if schemas are “best”, Java pipelines still need POJOs for cases like:

* **KV objects**
* **state handling**
* integration points where types are more convenient than `Row`

### The problem with hand-built POJOs

You must implement:

* `equals()`
* `hashCode()`
  (and often `toString()`)
  Manually doing this is error-prone and leads to subtle bugs.

### Best practice

Use **AutoValue** to generate POJOs:

* consistent equals/hashCode
* less boilerplate
* widely used in Beam codebase

Bonus: can be used with schemas via **@DefaultQA-style annotation** (as per transcript: “add @DefaultSchemas annotation”).

---

# 5) JSON best practices (Dataflow)

### Common need

Clickstream and event pipelines often ingest JSON strings.

### Recommended conversions

* JSON → **Row**: use **`JsonToRow`** (built-in transform)
* JSON → **POJO (AutoValue)**:

    * register a schema for the type (via **@DefaultSchema**)
    * then use **Convert utilities** to convert into the POJO

### Key operational point

JSON structure changes frequently → expect failures.
So: apply the **dead-letter pattern** for messages that don’t match expected schema.

---

# 6) DoFn lifecycle for micro-batching (external APIs)

### Problem

Calling an external API per element can overwhelm it.

### Best practice

Use DoFn lifecycle hooks to do **micro-batching**:

* initialise/reset batch in **`@StartBundle`**
* commit/flush batch in **`@FinishBundle`**

Important nuance:

* Bundles are a runner concept; **StartBundle/FinishBundle may run multiple times**.
* Always **reset variables correctly** so batch boundaries don’t leak across bundles.

---

# 7) DAG optimisation + general pipeline guidelines

### Filter early (reduce volume upstream)

Move volume-reducing steps as early as possible:

* fewer bytes through the pipeline
* lower cost and better performance
  This includes reducing before heavy operations like windows/aggregations (even if `Window` itself is “light”, it sets up later stages).

### Prefer serial transformations where possible

Applying transforms serially can let Dataflow:

* **fuse/merge into a single stage**
* reduce network IO between stages
* run more work on the same workers

### External systems & backpressure

If you read from or write to external systems (Bigtable, key-value lookups, sinks):

* they can become the bottleneck
* insufficient capacity → **backpressure** → pipeline slows

Best practice:

* provision external systems for expected peak volume
* enable autoscaling so Dataflow can scale appropriately (and also scale down when needed to avoid wasted resources)

---

# Quiz 6 — answers

### 1) Which functions of the DoFn lifecycle are recommended for micro-batching?

✅ **startBundle and finishBundle**

### 2) Choose all applicable options (external systems)

✅ **It is important to provision those external systems appropriately (i.e., to handle peak volumes).**
✅ **Not provisioning external systems appropriately may impact the performance of your pipeline due to back pressure.**
❌ “Testing external systems against peak volume is not important.”
❌ “External system doesn’t impact performance…”

### 3) Recommended way to convert JSON objects to POJOs?

From the choices given:
✅ **Use JsonToRow** (as the recommended built-in conversion step; then you can Convert/Map to POJO as needed)
