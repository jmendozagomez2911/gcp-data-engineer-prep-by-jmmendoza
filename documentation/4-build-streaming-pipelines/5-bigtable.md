# 🧠 Module — Bigtable: The Operational Serving Layer (Low-latency apps, schema design, reverse ETL, monitoring, Lab + Quiz)

In the streaming pipeline you’ve built so far, you already have:

* **Pub/Sub** = high-throughput “front door” ingestion
* **Dataflow** = real-time processing brain (event time, windows, state)
* **BigQuery** = analytical engine (ad-hoc SQL, historical analysis)

Bigtable enters when the question changes from **“analyse”** to **“serve”**:

✅ *“How do I power live leaderboards, in-game features, or real-time APIs with single-digit ms latency and millions of lookups?”*
→ That’s operational serving. That’s **Bigtable**.

---

## 1) 🎯 Why Bigtable exists in the pipeline

### The problem BigQuery cannot solve well

BigQuery is excellent for analytics, but it is not designed to be a **hot path** database for an application that needs:

* **single-digit millisecond** reads,
* **millions of reads per second** (RPS),
* predictable latency under spikes,
* key-based lookups like `get(user_id)` / `get(game_id)` / `get(leaderboard_id)`.

BigQuery can return results fast for SQL, but not as a “serve every request in < 20ms” operational store at massive QPS.

### Bigtable’s role

Bigtable is the **serving database** for “already-computed” results:

* store **pre-materialised** leaderboards, user features, recent history,
* enable instant retrieval by primary key / prefix,
* keep latency predictable as traffic grows.

**Mental model:**
BigQuery answers *questions* (SQL scans).
Bigtable answers *lookups* (row-key reads).

---

## 2) 🧩 Core concepts: Bigtable’s data model (first principles)

Bigtable is a **wide-column / key-value** store. It’s optimised around one core primitive:

### ✅ Row key = the primary index

* The **row key** is the main way you retrieve data efficiently.
* Rows are stored **sorted by row key** (lexicographic order).
* The row key design determines:

    * distribution across nodes,
    * hotspot risk,
    * range scan efficiency,
    * whether your “top queries” are fast or painful.

### Wide-column model basics (what matters for exams and real life)

* A table is sparse: you can have many columns, but most rows have few populated cells.
* Data is grouped into **column families** (think: “coarse grouping for storage/IO characteristics”).
* Within families you have **column qualifiers** (dynamic columns).
* Cells are **versioned by timestamp** (useful for “latest N values” patterns).

**Exam reality:** Bigtable schema is not about “normalisation”; it’s about designing for the *few access patterns you must guarantee*.

---

## 3) 🆚 BigQuery vs Bigtable: exam decision matrix

### ⚡ When to choose Bigtable

Choose **Bigtable** when you see:

* “**single-digit ms** latency”
* “**millions of lookups** / RPS”
* “serve to a game/app/API”
* “online feature store / real-time predictions”
* “IoT/time-series with high ingest + fast recent reads”
* “need linear scaling by adding nodes”

### 🧠 When to choose BigQuery

Choose **BigQuery** when you see:

* “ad-hoc analysis / unknown questions”
* “joins, group-bys, large scans”
* “petabyte analytics”
* “BI dashboards over historical data”
* “training datasets / offline ML preprocessing”

### 🔁 The combined pattern (very common)

* **BigQuery stores history and supports analysis**
* **Bigtable stores operational projections** for apps
  Example: compute leaderboard scores (BigQuery / Dataflow) → push “current leaderboard state” into Bigtable for instant reads.

**If you see**: “analyse in BigQuery but serve in real time”
→ **BigQuery + Bigtable serving layer** (reverse ETL pattern).

---

## 4) 🏗️ Architectural patterns with Bigtable (and when to use each)

### Pattern A — 🏎️ Real-time serving layer (“materialised operational view”)

**Use when:** the app needs fast reads of “current state”.

* Store per-game leaderboard rows
* Store per-user “recent actions”
* Store per-team aggregated stats

**Typical flow:**
Pub/Sub → Dataflow (or BigQuery continuous queries) → **Bigtable** → application reads

**Exam trigger:** “live leaderboard / real-time API / fan experience” → Bigtable.

---

### Pattern B — 🧬 Online feature store for ML serving

**Use when:** a model needs **real-time feature lookup** per event.

* Vertex AI endpoint receives a transaction
* needs user history + recent behaviour under strict latency
* reads features from Bigtable, predicts, returns decision

**Exam trigger:** “online predictions”, “feature lookups”, “fraud detection real-time inference” → Bigtable.

---

### Pattern C — ⏱️ Time-series ingestion + recent reads

**Use when:** high ingest and you often query “latest values” per device/user.

* IoT sensors
* clickstream
* telemetry

Key success factor: **row key design** to avoid hotspots (more below).

---

### Pattern D — 📊 Analytics on Bigtable without moving data (limited use)

The module mentions interacting/analyzing Bigtable data via:

* programmatic access (e.g., HBase client),
* SQL-style interaction / federated analysis from BigQuery (when supported in your setup).

**Exam trap:** This does *not* make Bigtable a replacement for BigQuery.
Bigtable is still not where you do heavy joins and ad-hoc analytics.

---

## 5) 🔑 Schema design: “Speed through simplicity” (what really decides performance)

### The golden rule

**Design the row key for your top read patterns.**
Because Bigtable’s performance is largely:
**row-key lookup + locality**.

### What breaks in real life: hotspotting

If all writes land in the same key range, one node/tablet gets overloaded.

**Classic mistake (time-series):**
Row key starts with a monotonically increasing timestamp like:
`2025-09-15T10:30:00Z#sensor_123`

Why it breaks:

* new events all go to the “end” of the keyspace,
* one region/tablet becomes the hot write target,
* throughput collapses even if you have many nodes.

**Better strategies (conceptual):**

* Start with an entity key (user/device), then time:

    * `sensor_123#<reversed_timestamp>`
* Or add a hashed/salted prefix when you need write spreading.

### Wide-row pattern for “latest N items”

If you need “customer profile + last 5 transactions” under 20ms:

* row key = `customer_id`
* profile in one family
* transactions stored as multiple cells/columns in another family
* fetch the row and request only the latest N cells

This pattern is heavily exam-friendly because it uses:
✅ single row read
✅ no scans
✅ no joins
✅ stable latency

---

## 6) 🔄 Moving data between BigQuery and Bigtable (reverse ETL operationalisation)

### Why it’s hard

BigQuery is structured/tabular. Bigtable is wide-column.
So you must map:

* *rows/columns* → *row key + families + qualifiers*

### The key design principle

Combine the fields that are used most often in `WHERE` lookups into the **row key** (because the row key is a primary index).

**Exam cue:** “optimise Bigtable lookups” → design row keys around query predicates.

---

## 7) 🧰 Integration points: Dataflow templates and change streams

The module calls out a pragmatic point:

* you *can* write pipelines from scratch,
* but **Dataflow templates** simplify common ingestion/movement tasks.

Specifically mentioned templates processing continuously:

* **Bigtable change streams → BigQuery**
* **Bigtable change streams → Pub/Sub**

**Exam trigger:** If the question is “replicate Bigtable changes into analytics / messaging without writing custom code” → Dataflow templates.

---

## 8) 📈 Monitoring + troubleshooting (what breaks and how you isolate it)

### Monitoring: what you’re trying to detect

* rising p95/p99 latency,
* uneven load (hot keys/tablets),
* insufficient node capacity for QPS,
* application-level misuse (too many connections, scans).

### Troubleshooting playbook (from the module’s flashcards)

1. **Prove whether Bigtable is the bottleneck**

* Temporarily comment out Bigtable reads/writes.
* If performance improves → your Bigtable usage/schema/client pattern is likely wrong.
* If not → bottleneck elsewhere.

2. **Reuse one long-lived connection**

* Opening Bigtable connections is expensive.
* Use one shared long-lived client/connection; it multiplexes across threads.
  **Real-life failure mode:** opening per-request connections → latency spikes + resource exhaustion.

3. **Ensure reads/writes spread across many rows**

* If you hammer a small number of row keys, nodes can’t share the work.
  **Symptom:** throughput doesn’t scale even after adding nodes.

4. **Compare read vs write performance**

* Reads *much faster* than writes can indicate odd patterns like:

    * scanning large key ranges with few matching rows,
    * reading nonexistent keys,
    * poor range scan design.

---

## 9) 🧱 Proactive isolation: Bigtable Data Boost (exam-friendly concept)

Bigtable Data Boost is presented as:
✅ run analytics/batch jobs on production data **without impacting** primary serving traffic.

Core idea:

* isolate batch/analytic compute from serving cluster traffic,
* use serverless compute that reads underlying storage,
* pay only for what you use.

**Exam trigger:** “run analytical/batch workloads on Bigtable data without harming app latency” → **Data Boost**.

---

# 🧪 Lab — Monitor E-sports Chat with Streamlit (Pub/Sub → BigQuery → Gemini → BigQuery continuous queries → Bigtable → Streamlit)

This lab is important because it stitches together several “new-ish exam patterns”:

* BigQuery **continuous queries**
* `APPENDS()` TVF for incremental processing
* BigQuery ML **remote model** (Gemini) used inside SQL
* exporting continuously from BigQuery into **Bigtable**
* Bigtable serving used by an app (**Streamlit**)

---

## 🧭 What you build (end-to-end flow)

1. Python generates chat events → publishes to **Pub/Sub**
2. Pub/Sub subscription **writes directly to BigQuery** table `raw_chat_messages`
3. BigQuery remote model (Gemini via Vertex AI) classifies messages
4. Continuous query writes only **unsportsmanlike** into `unsportsmanlike_messages`
5. Another continuous export pushes unsportsmanlike rows into **Bigtable**
6. **Streamlit app** reads Bigtable and presents moderation UI

**Conceptual lesson:** BigQuery can do near-real-time SQL + ML classification, but Bigtable is where you serve results to an interactive app with predictable latency.

---

## 🧱 Resources created (high-yield)

* BigQuery dataset: `esports_analytics`
* BigQuery tables:

    * `raw_chat_messages` (streamed from Pub/Sub)
    * `unsportsmanlike_messages` (partitioned by `timestamp`)
* Bigtable:

    * instance `instance`
    * table `unsportsmanlike`
    * column family `messages`
* Pub/Sub:

    * topic `esports_messages_topic`
    * subscription `esports_messages_topic-sub` (delivery: **Write to BigQuery**)
* BigQuery connection for Vertex AI remote models (`Region.esports_qwiklab`)
* BigQuery remote model: `esports_analytics.gemini_model`

---

## 🔐 IAM / permissions gotchas (these are exam-grade)

### Gotcha A — Pub/Sub → BigQuery subscription needs BigQuery permissions

When subscription delivery is “Write to BigQuery”, Pub/Sub uses a service account like:
`service-<PROJECT_NUMBER>@gcp-sa-pubsub.iam.gserviceaccount.com`

You must grant it **BigQuery Data Editor** (dataset-level in the lab), otherwise you see errors about missing:

* `bigquery.tables.get`
* `bigquery.tables.updateData`

**Exam trigger:** “Pub/Sub subscription cannot write to BigQuery table” → fix IAM for Pub/Sub service account.

---

### Gotcha B — Your publisher (compute SA) needs Pub/Sub Publisher

The generator script runs as the compute service account:
`<PROJECT_NUMBER>-compute@developer.gserviceaccount.com`

It must have **Pub/Sub Publisher** on the topic.

**Trap called out explicitly:** do *not* pick Pub/Sub Lite roles.

---

### Gotcha C — BigQuery remote model needs Vertex AI permissions

Creating a **BigQuery connection** generates a service account like:
`bqcx-<PROJECT_NUMBER>@gcp-sa-bigquery-condel.iam.gserviceaccount.com`

That principal must have **Vertex AI User**.

**Exam trigger:** “BigQuery remote model / connection permission denied” → grant Vertex AI User to the BigQuery connection service account.

---

## 🧠 Continuous queries: why `APPENDS()` is non-negotiable here

BigQuery continuous queries are designed to process **new rows** without rescanning the whole table.

`APPENDS(TABLE raw_chat_messages)` means:

* “only read rows appended since last execution”
* avoids repeated full scans
* prevents duplicate reprocessing (in the intended design)

**Exam trigger:** “incremental continuous processing in BigQuery” → `APPENDS()` TVF.

---

## 🤖 Gemini classification inside SQL (what the lab teaches conceptually)

The continuous query uses `ML.GENERATE_TEXT()` with a prompt that forces output:

* only `sportsmanlike` or `unsportsmanlike`

Then filters:

* `WHERE ml_generate_text_llm_result = 'unsportsmanlike'`

**Operational gotcha:** LLM output must be constrained; otherwise you’ll get messy text and your filter breaks. This lab avoids that by strict prompt instructions.

---

## 📤 Export to Bigtable continuous query: the “priority trap”

The export uses:

```sql
EXPORT DATA OPTIONS (format='CLOUD_BIGTABLE', ...)
AS SELECT ... FROM APPENDS(TABLE esports_analytics.unsportsmanlike_messages)
```

You hit an error:

* Bigtable app profile must be **LOW priority**
* default profile is **HIGH priority**

Fix:

* edit Bigtable **Application Profile** → routing priority **Low**

**Why this exists (first principles):**
You’re running an ongoing export job (an analytic-style pipeline). Bigtable wants that traffic to be deprioritised so it doesn’t harm serving latency.

**Exam trigger:** “EXPORT DATA to Bigtable fails due to app profile priority” → set profile priority to LOW.

---

## 🖥️ Streamlit serving layer (what matters)

Streamlit app:

* connects to Bigtable
* displays flagged messages
* simulates moderator actions (ban/suspend/dismiss)

**Conceptual lesson:** Bigtable is the operational backing store for user-facing tools and dashboards that need fast, repeated reads.

---

# ✅ Quiz integration (review question + Quiz 4)

## 🧪 Review question: Vertex AI fraud detection + Bigtable (Select all that apply)

**Correct choices:**

* ✅ Bigtable can be used as a low-latency online feature store for real-time predictions.
* ✅ Bigtable’s high write throughput supports massive-scale streaming ingest.
* ✅ Bigtable scales horizontally to maintain low-latency lookups as load grows.

**Incorrect:**

* ❌ “Bigtable is primarily used for complex SQL queries to train the model.”
  Training/offline analytics → **BigQuery** (or other analytics systems), not Bigtable’s sweet spot.
* ❌ “Vertex AI includes a built-in Bigtable instance.”
  You provision Bigtable separately; Vertex AI does not magically embed it.

**Decision rule:**
If the model needs **online features under strict latency** → Bigtable.
If you need **training datasets / joins / heavy SQL** → BigQuery.

---

# 🧪 Quiz 4: Bigtable

## Q1) Best schema for <20ms: profile + 5 most recent transactions

**Correct answer:** ✅ **Single wide table with row key = customerID**; profile family + transactions family; fetch with one ReadRows and request last 5 cells.

**Why it’s correct (first principles):**

* one primary-key read (fast),
* no scans,
* no joins,
* “latest N” is handled via cell versions / per-row retrieval controls.

**Why the others are traps:**

* “customerID#transactionID + scan prefix + limit(5)” → ❌ scans are slower and less predictable than a single key read.
* “BigQuery then scheduled export” → ❌ adds latency; not real-time.
* “two tables profiles + transactions” → ❌ two reads + application join; more latency and more failure points.

**Exam trigger:** “profile + recent N items under strict latency” → **one row per entity** pattern.

---

## Q2) Valid/recommended methods to analyse or interact with Bigtable (Select all that apply)

**Correct answers:**

* ✅ Use the native HBase client for programmatic access.
* ✅ Execute a federated query from BigQuery for interactive SQL analysis *when supported/configured*.
* ✅ Serve low-latency requests for user-facing dashboards (last N actions, etc.).

**Incorrect / trap:**

* ❌ “Performing a JOIN with a customer dimension table using the Bigtable SQL interface.”
  Bigtable is not a join engine. If joins are central, you chose the wrong primary store → **BigQuery**.

**Decision rule:**
If the requirement says **JOIN** / star schema analytics → BigQuery.
If it says **fast keyed lookups** / “last N actions” → Bigtable.

---

## Q3) Row key for time-series should always start with timestamp

**Correct answer:** ✅ **False**

**Why false:**
Starting with timestamp is a classic hotspot pattern:

* new writes cluster into the same key range,
* load won’t distribute evenly across nodes,
* throughput and latency degrade.

**Better exam-friendly reasoning:**
Row keys must distribute writes while still supporting your query patterns:

* entity-first keys (device/user) + time component,
* reversed timestamps or salted prefixes when needed.

**Decision rule:**
If writes arrive in time order at high volume → **avoid monotonically increasing prefixes** in the row key.

---

# 🧾 Bigtable exam decision rules (memorise)

1. **Need single-digit ms lookups at massive scale** → **Bigtable**
2. **Need ad-hoc SQL / joins / scanning historical data** → **BigQuery**
3. **Need real-time serving of analytics results** → compute in BigQuery/Dataflow → **materialise into Bigtable**
4. **Need online ML feature lookup** → **Bigtable** (online store)
5. **Time-series ingest + recent reads** → Bigtable, but row key must avoid hotspotting
6. **Performance issues** → first check schema + connection reuse + hotspot distribution
7. **Need batch/analytics on Bigtable without hurting serving traffic** → **Bigtable Data Boost**
8. **BigQuery continuous queries incremental processing** → use `APPENDS()`
9. **Continuous export to Bigtable fails due to priority** → set Bigtable app profile to **LOW**

---
