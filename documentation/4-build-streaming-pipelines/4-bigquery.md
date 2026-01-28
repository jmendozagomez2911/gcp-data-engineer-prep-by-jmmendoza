# 🧠 Module — BigQuery: The Analytical Engine (Streaming analytics, Continuous Queries, Performance, Lab)

At this point in the pipeline, Pub/Sub (or Kafka) is your **front door**, Dataflow is your **processing brain**, and BigQuery becomes your **analytical engine**—the place where you answer questions at scale using SQL, even when the query pattern is not known in advance.

BigQuery is the right fit when:

* you need to run **ad-hoc SQL** on large datasets,
* you want **fast analytics** (seconds over TB/PB),
* you care about **cost control** via scan reduction (partitioning/clustering),
* and you want to operationalise results (Reverse ETL, continuous queries).

---

## 1) Why BigQuery sits here in the streaming journey

### The “after Dataflow” question

Once Dataflow has parsed/enriched/aggregated, you need somewhere that can:

* store huge amounts of data cheaply,
* run SQL quickly,
* support many analysts and dashboards,
* and scale without you managing infrastructure.

BigQuery does that because it’s:

* **serverless**
* **massively parallel**
* **columnar** (optimised for analytics, not point lookups)
* capable of querying **petabytes** of prepared streaming data fast.

In the Grand Prix context, BigQuery answers questions like:

* “Which drivers have consistent lap times?”
* “Correlation between gear changes and incidents?”
* “Weather effect on outcomes?”

Those are *analytics questions*, not serving questions (that’s Bigtable territory).

---

## 2) BigQuery ingestion: Batch vs Streaming (and the exam decision rule)

### Batch ingest

**When:** freshness isn’t critical (hourly/daily), big bounded loads.
**How:** load in chunks.
**Tools:** BigQuery load jobs, Data Transfer Service (DTS), Spark/Hadoop connectors.
**Trade-off:** cheap/scalable, but you accept latency.

### Streaming ingest

**When:** dashboards, anomaly detection, operational analytics need data within seconds.
**How:** row-by-row or micro-batches.
**Tools:** Storage Write API, Pub/Sub → BigQuery subscription, Dataflow.
**Trade-off:** more expensive per GiB, but low latency.

**High-yield exam rule:**

* If the requirement screams **“within seconds” / real-time dashboards”** → streaming ingest.
* If it says **“nightly loads / historical logs / cost minimisation”** → batch ingest.

---

## 3) ETL vs ELT: why BigQuery shifts the default

### Traditional ETL

Transform before loading into the warehouse:

* Dataflow / Spark does transformations
* BigQuery stores final shaped data

### Modern ELT with BigQuery

Load raw data *first* into BigQuery, then transform **in-place using SQL**.

Why BigQuery pushes ELT:

1. simplifies pipeline (less external processing for many cases),
2. BigQuery compute is powerful and serverless,
3. keeps raw data → re-run transformations later without re-ingesting.

**Practical meaning:**

* For many transformations, you can skip Dataflow entirely and use BigQuery SQL (especially batch ELT).
* For **stateful streaming** and messy event-time problems, Dataflow still wins.

---

## 4) Storage Write API (why they mention it)

For streaming ingestion, the **BigQuery Storage Write API** is presented as the preferred modern mechanism (high throughput, fewer legacy limitations).

**What you should take away:**

* “Legacy insert API has row/throughput constraints.”
* “Storage Write API is the preferred choice for new streaming ingestion.”

Exam-style trigger:

* If they ask “best way to ingest high-throughput streaming into BigQuery” → Storage Write API patterns.

---

## 5) DTS (Data Transfer Service): what it is *and what it is not*

**DTS is NOT streaming.**
It’s scheduled batch transfers, “set it and forget it”.

Use it when:

* you need recurring pulls from supported sources,
* like Ads/YouTube or cloud storage (including S3) on a schedule.

If the question says “scheduled import every day/hour from a supported SaaS/source” → DTS.

---

## 6) Change Data Capture (CDC): the “near-real-time database mirror” pattern

CDC is used when you want **a near-instant reflection of a database’s changes** in BigQuery:

* inserts
* updates
* deletes

**Core idea:** you stream *changes*, not full reloads.

**Exam cue:** If they say “mirror production DB changes in BigQuery, including deletes” → CDC design pattern.

And the module is very clear about the boundary:

* CDC keeps tables current,
* but **complex transformations/stateful logic** still points you to **Dataflow**.

---

## 7) Dataflow vs BigQuery Continuous Queries (super important distinction)

### Dataflow

Beam pipelines, sophisticated intermediary:

* handles complex transforms
* windowing/watermarks/state
* can write dynamically to multiple tables or other sinks

### BigQuery Continuous Queries

Long-running SQL that processes new rows as they arrive in a source table and writes results to:

* another BigQuery table
* Pub/Sub topic
* Bigtable table
* Spanner table

They’re positioned as:

> “a lightweight, SQL-native ETL tool inside BigQuery”

**But with two key constraints you must remember (they’re exam bait):**

1. **Continuous queries require BigQuery reservations** (slots).
2. They’re designed to be **stateless**.

So the decision rule is:

* Need **stateful streaming semantics** (late data refinement, session windows, complex joins, event-time correctness)? → **Dataflow**
* Need **simple real-time transformations/filters/derivations** expressed in SQL, low engineering overhead, and you can live with statelessness? → **Continuous queries**

---

## 8) Reverse ETL (BigQuery → operational systems)

Reverse ETL = push analytics results back into systems that *run the business*.

In this module, they connect reverse ETL strongly with continuous queries:

Workflow:

1. Write SQL
2. Run continuously
3. Output is continuously pushed to destinations like:

    * Pub/Sub (trigger apps)
    * Bigtable (serve state)
    * Spanner (serve app users without hammering BigQuery)
    * other BigQuery tables (pipeline chaining)

**Exam cue:** If data starts in BigQuery and ends up powering apps/CRM/alerts → reverse ETL.

---

## 9) Continuous queries + “agentic AI” demo (what they want you to learn)

The security demo isn’t about details; it’s about recognising a new architectural pattern:

* streaming events land in BigQuery
* a continuous query identifies something interesting
* it triggers an agent workflow (ADK) using LLM prompts
* results get logged back to BigQuery for audit + feedback loops

Takeaway for exam thinking:

* BigQuery isn’t only “warehouse”; it can be **event-driven** when paired with continuous execution outputs.

---

## 10) Performance design: columnar storage, partitioning, clustering

### Columnar storage (why it’s fast for analytics)

BigQuery stores columns separately, so analytical queries that scan a few columns can avoid reading everything.

### Partitioning

Splits a table into chunks (often by date/timestamp).
Main goal: **prune partitions** → scan less data → cheaper and faster.

### Clustering

Organises data *within partitions* by chosen columns to reduce scan inside partitions (better pruning for common filters/group-bys).

**Key clarification (directly tied to the review question):**

* Partitioning = segment by time (or int range) into partitions
* Clustering = co-locate similar values for faster pruning/filtering within partitions
  It is **not** “segment data by date”—that’s partitioning.

So the review statement:

> “The primary purpose of clustering is to segment by date/timestamp column to prune partitions”

✅ **False** (that’s partitioning, not clustering)

---

## 11) Enforcing cost control: require partition filter (high-yield exam item)

If you have a partitioned table and want to **force queries to filter on the partition column**, the most direct enforcement is:

✅ **Table-level partition filter enforcement** using the table property `--require_partition_filter`

Why this is the correct one:

* It’s enforced at the **table level**
* queries fail if they don’t include the filter
* prevents accidental full scans

Why the others are weaker:

* views are bypassable if users can access the base table
* IAM doesn’t inspect SQL query content
* reservations control compute capacity, not query predicates

---

# 🧪 Lab: Stream e-sports data with Pub/Sub → BigQuery (no Dataflow)

This lab is intentionally “lighter” than the Dataflow lab. It demonstrates the idea:

> “What if I want real-time ingestion and analytics using mostly configuration + SQL?”

### What you build

1. **BigQuery dataset + table** (`esports_analytics.raw_events`) with a defined schema
2. **Pub/Sub topic + subscription**
3. Configure subscription delivery type: **Write to BigQuery**
4. Fix IAM so Pub/Sub can write to BigQuery
5. Run Python simulator to publish events
6. Query raw table + build leaderboard views in SQL

### The two IAM “gotchas” they want burned into your brain

#### Gotcha A: Pub/Sub service account needs BigQuery permissions

When you switch subscription delivery to “Write to BigQuery”, Pub/Sub uses a Google-managed service account like:

`service-<PROJECT_NUMBER>@gcp-sa-pubsub.iam.gserviceaccount.com`

You must grant it dataset/table permissions (they use **BigQuery Data Editor** at the dataset level in the lab).

**Exam cue:** “Subscription can’t write to BigQuery; missing bigquery.tables.updateData” → fix IAM for Pub/Sub SA.

#### Gotcha B: Your compute service account needs Pub/Sub Publisher

Your simulator (running under the compute principal) must be able to publish to the topic.

**Exam cue:** “Permission denied publishing to topic” → grant **Pub/Sub Publisher** to the compute service account.

---

## 12) The SQL leaderboard patterns (what they’re training you to recognise)

### Player leaderboard view

They compute a score from raw events:

* score = 5 for `match_end`, else 1 for `player_elimination`
* group by `winner_player_id`
* rank by total score
* last_updated = max(timestamp)

Key SQL ideas:

* `CASE WHEN` inside `SUM()` to create weighted scoring
* `RANK() OVER (ORDER BY …)` for leaderboard
* `MAX(timestamp)` for “latest update time”

### Team leaderboard view

* count match wins (`event_type='match_end'`)
* group by `winner_team_id`
* rank by `COUNT(*)`

**Exam cue:** If you see “leaderboard”, they love window functions (`RANK()`) and aggregation patterns.

---

# ✅ BigQuery module exam decision rules (memorise)

1. **Need ad-hoc analytics, unknown query patterns, massive scale** → BigQuery
2. **Need low-latency serving reads for apps** → Bigtable (BigQuery is not a serving DB)
3. **Need real-time ingestion into BigQuery with minimal processing** → Pub/Sub → BigQuery subscription (or Storage Write API)
4. **Need complex streaming transforms, state, late data correctness** → Dataflow
5. **Need simple real-time SQL transforms and push results out** → BigQuery continuous queries (requires reservations; stateless)
6. **Need scheduled loads from supported external systems** → BigQuery DTS
7. **Need DB change stream including updates/deletes into BigQuery** → CDC pattern
8. **Need to prevent accidental full scans on partitioned tables** → `require_partition_filter`

---

If you paste the next module (Bigtable or Looker/serving layer), I’ll connect it directly to this one with a clean rule: **BigQuery for analytics vs Bigtable for serving**, and how Dataflow/continuous queries feed Bigtable.
