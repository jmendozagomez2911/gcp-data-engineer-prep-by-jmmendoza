# ⚡️ Module — Dataflow: The Processing Powerhouse (Deep Dive + Quiz 2)

You’ve already chosen your **ingestion layer** (Pub/Sub / Managed Kafka). Now you need the part that turns a chaotic stream into **correct, usable, queryable data** in real time. That’s what Dataflow is for.

Dataflow = **managed Apache Beam runner**. Beam is the programming model; Dataflow is the managed execution engine that:

* parallelises work,
* manages state,
* deals with late/out-of-order data,
* autosscales,
* and writes efficiently to sinks like BigQuery/Bigtable.

The module is basically teaching you: **“streaming isn’t hard because data is big; it’s hard because data is time-dependent and unreliable.”**

---

## 1) What Dataflow actually “does” (beyond marketing)

### Dataflow’s job in a streaming pipeline

You can think of the pipeline as 3 layers:

1. **Ingestion (Pub/Sub / Kafka)**
   “Get events in reliably, buffer them, decouple producers/consumers.”

2. **Processing (Dataflow)**
   “Parse, validate, enrich, aggregate, order by event-time, handle late data, keep state.”

3. **Serving/Analytics sinks (BigQuery / Bigtable / etc.)**
   “Store results for dashboards, applications, ML, audits.”

So Dataflow is where you solve the hard parts:

* **out-of-order events**
* **late events**
* **stateful aggregations**
* **exactly-once-ish behaviour (no loss / no double processing)**
* **high-throughput sink writing**

---

## 2) “Complex data arrives from Pub/Sub” — why it matters

The transcript gives you 3 data “shapes” because each demands different transforms.

### A) JSON payloads (raw, unstructured-ish)

Example: raw gameplay events as JSON strings.

**Why this is painful in streaming:**

* JSON is text → parsing is CPU heavy.
* fields may be missing / malformed.
* timestamps are inside the payload → you must extract them to do event-time processing.

**Dataflow move:**
Use **ParDo** to:

* parse JSON
* extract `event_timestamp`
* convert into a **structured** representation (Beam Row / typed object)

**Conceptually:** you’re turning “string blobs” into “typed events”.

---

### B) Schemas (the blueprint)

A schema is not the data; it’s the **rules** of what the data should look like.

**Why the module mentions Avro/Protobuf:**

* JSON is flexible but heavy and error-prone.
* Protobuf/Avro = compact binary + schema-driven → better at high volume.

**Dataflow move:**
Instead of parsing JSON, you decode binary records using:

* a custom decoder, or
* Beam schema transforms

**Exam angle:** If they say “high-volume streaming, compact encoding, strict schema” → Protobuf/Avro is the implied best practice.

---

### C) Structured transaction events (already “clean”)

Example: in-game purchases.

These are typically:

* already structured
* stable schema
* low parsing need

**Main streaming work here = enrichment**, not parsing.

**Dataflow move:**
Join/enrich events with reference data (often from Cloud Storage, Bigtable, etc.) to add context:

* item category
* power level
* region
* player tier

This is how raw events become “analytics-ready”.

---

## 3) Dataflow processing strategies (the core mechanics)

The transcript highlights 3 because these are the pillars of Dataflow’s “magic”:

### 3.1 Parallel processing (bundles)

Beam makes you think “DoFn runs per element”, but the runner optimises:

* Dataflow groups elements into **bundles**.
* Then applies your logic over a buffered set.
* This reduces overhead (function calls, context switching).

**Why it matters:**
High-throughput streaming dies from overhead, not just from “big data”. Bundling is one reason Dataflow can scale.

**Exam trap:** People think “my DoFn is called once per event”. Conceptually yes; practically, Dataflow executes in batches internally for efficiency.

---

### 3.2 Windows + triggers (how streaming becomes “queryable”)

Streaming has no natural “end”. So you need to decide:

* **What slice of time do we aggregate?** → *Windowing*
* **When do we emit results?** → *Triggers*
* **What about late events?** → *Allowed lateness*
* **Do we output incremental refinements or snapshots?** → *Accumulation mode*

#### The key real-world problem

Events arrive out of order. If you aggregate by arrival time, your numbers are wrong.

So Dataflow processes in **event time** (timestamp embedded in the event) using:

* **Watermarks:** Dataflow’s estimate of how far the stream has progressed in event time
  (“the shared game clock”)

* **Allowed lateness:** How long you keep a window open for late events

* **Triggers:** When to output intermediate/final results

* **Accumulation mode:**

    * **Discarding:** after a firing, forget what you had; next firing is a fresh delta
    * **Accumulating:** keep previous state; late data updates the existing result

**Big idea:** Streaming outputs are often *provisional* then later *corrected*.

That “photo finish toolkit” in the transcript is exactly this.

---

### 3.3 Data sinks (BigQuery vs Bigtable)

A sink is just “where results go”, but the details matter.

#### BigQuery sink (analytics)

Dataflow uses **high-throughput APIs** (the transcript mentions Storage Write API). Translation:

* it’s designed for scale
* not the old row-by-row insertion style

**Use BigQuery when:**

* dashboards / SQL analytics
* history / audit
* aggregation queries

#### Bigtable sink (serving)

Dataflow writes **mutations** (row updates) keyed by row key (e.g., `player_id`).

**Use Bigtable when:**

* low-latency reads for apps
* “current state per key”
* leaderboards, profiles, real-time app lookups

**This is the important distinction:**

* BigQuery is for *analysis*
* Bigtable is for *serving*

---

## 4) How Dataflow “solves” the Galactic Grand Prix challenges

### Latency + ordering

**Problem:** events from different regions arrive out of order.

**Dataflow tool:** event-time processing + watermarks

Meaning: Dataflow tries to produce correct time-based outputs even when arrival order is messy.

---

### Scalability

**Problem:** 10x surge in traffic.

**Dataflow tool:** serverless autoscaling

Meaning: you don’t resize clusters manually; Dataflow scales workers.

---

### Fault tolerance

**Problem:** a worker dies mid-stream.

**Dataflow tool:** streaming engine / persistent state

Meaning: state is not lost, tasks can be reassigned, and you avoid duplicate/lost processing.

---

### Data quality + completeness (the “best combined example”)

Two separate issues:

1. **Corrupted record** → don’t crash pipeline
   Solution: “dead-letter pattern” in Beam (route bad data away)

2. **Delayed winner event** → don’t announce wrong winner
   Solution: allowed lateness + triggers

    * output provisional
    * late event arrives
    * trigger fires again
    * output corrected result

This is the “speed + correctness” story. Streaming is never only one.

---

## 5) Windowing types (and when each is right)

### Fixed windows (tumbling)

* Non-overlapping chunks: e.g., every 60 seconds
* Great for periodic dashboards (“gold earned per minute”)

**Mental model:** “report every minute.”

---

### Sliding windows (hopping)

* Overlapping windows
* Example: “last 3 minutes, updated every 15s”

**Mental model:** “rolling average.”

---

### Session windows

* Group events until a gap of inactivity occurs
* Perfect for “AFK/disconnected” detection

**Mental model:** “group bursts of activity.”

---

## 6) Dataflow ML (what they want you to understand)

They mention RunInference + MLTransforms to show:

* Dataflow can run pre-trained models inside the pipeline
* You can enrich data + do real-time inference
* Output alerts or tables for moderation/action

**Exam cue:** If it’s “model inference in stream” → Dataflow + Vertex AI / model handler patterns.

---

# ✅ Quiz 2: Dataflow — answers with reasoning

## Q1) Detect disconnect if >2 min inactivity between actions

✅ **Session Windows with a 2-minute gap duration**

Why:

* You want to group actions until there’s a *gap* of 2 minutes.
* That’s literally the definition of session windows.

---

## Q2) Watermark guarantee no earlier timestamps can ever be processed

✅ **False**

Why:

* Watermarks are an **estimate**, not a guarantee.
* Late data can still arrive and be processed if allowed lateness is configured.
* The watermark passing a time means “we *expect* most data before that has arrived.”

This is a classic trick question.

---

## Q3) Which challenges Dataflow solves (select all that apply)

✅ Correct:

* **Correctly ordering events from different regions** (event-time + watermarks)
* **Autoscaling worker resources** (serverless scaling)
* **Ensuring data not lost or processed twice if worker fails** (fault tolerance / persistent state)

❌ Incorrect:

* Authenticating user identities on website (not Dataflow’s role)
* Generating raw JSON from clients (that’s the producer side)

---

# ✅ Review question at the end (Select three)

“Which statements accurately describe Dataflow?”

✅ Correct:

* **Unified programming model (Apache Beam) for batch + streaming with same code**
* **Serverless managed service that autosscales**
* **Strong consistency / exactly-once processing semantics** (this is how the course frames it; in exam questions, still watch wording like *delivery vs processing* and sink idempotency)

❌ Incorrect:

* Must provision/manage VM cluster
* Must be written only in Java
