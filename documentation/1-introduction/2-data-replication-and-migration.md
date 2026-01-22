# 📦🚚 **README — Module 03: Data Replication & Migration**

**Goal:** Pick the **correct ingestion lane** to bring data into Google Cloud based on **data size, bandwidth, and transfer type** (ad-hoc, scheduled, offline, CDC), and understand how **Datastream CDC** works end-to-end.

**Read me like this:** 1) baseline architecture → 2) tool selection by transfer type → 3) STS vs Transfer Appliance vs gcloud storage → 4) Datastream deep dive (CDC event structure + unified types) → 5) lab (Postgres → BigQuery) → 6) exam cheats + quiz mapping.

---

## 1) 🧭 Baseline Architecture (Replicate & Migrate stage)

**Purpose:** bring data from **external/internal systems into Google Cloud** so it can be **transformed** and ultimately **stored/served** in GCP.

**Typical origins**

* On-prem & multi-cloud: file systems, object stores (S3 / Azure Blob), HDFS
* Relational DBs: Oracle, MySQL, PostgreSQL, SQL Server
* Other formats / systems: NoSQL, non-relational sources (often via ETL tooling)

**Where it typically lands**

* **Cloud Storage** (landing zone / data lake)
* **BigQuery** (analytics sink; sometimes direct loads/replication)

---

## 2) 🚦Choose the Right “Lane” (transfer types)

Think in **four lanes**:

### A) 🔹 Ad-hoc online transfer (small to medium)

* Best when: you need to copy data **quickly**, manually or in scripts.
* Tool: **`gcloud storage cp`**
* Destination: **Cloud Storage**

```bash
gcloud storage cp ./file.csv gs://my-bucket/path/
```

> 💡 **Exam Tip**
> Keyword “**cp command**” + “ad-hoc transfer to Cloud Storage” → **gcloud storage**.

---

### B) 🔹 Large online transfer (managed + scheduled)

* Best when: large datasets online, repeated transfers, multi-cloud, HDFS.
* Tool: **Storage Transfer Service (STS)**
* Sources: on-prem, multicloud file systems, object stores (S3, Azure Blob), HDFS
* Destination: **Cloud Storage**
* Key feature: **scheduled transfers** + efficient large-scale movement

> 💡 **Exam Tip**
> “Move large datasets from S3/Azure/HDFS → GCS and schedule it” → **Storage Transfer Service**.

---

### C) 🔹 Massive offline transfer (bandwidth-constrained)

* Best when: **very large** datasets and/or **limited bandwidth**
* Tool: **Transfer Appliance**
* Pattern: Google ships hardware → you load data → ship back → data uploaded to GCS

> 💡 **Exam Tip**
> “Very large dataset + offline/limited bandwidth” → **Transfer Appliance**.

---

### D) 🔹 Continuous database replication (CDC)

* Best when: ongoing changes from relational DBs to analytics / event-driven use cases
* Tool: **Datastream**
* Sources: Oracle, MySQL, PostgreSQL, SQL Server (and supported GCP relational sources depending on setup)
* Destinations: **Cloud Storage** or **BigQuery**
* Supports:

    * **historical backfill** + **propagate new changes**
    * **selective replication** (schema/table/column)
    * optional processing with **Dataflow** before loading to BigQuery

> 💡 **Exam Tip**
> Keywords “CDC”, “near real-time replication”, “WAL/binlog/LogMiner/txn logs”, “schema/table/column selection” → **Datastream**.

---

## 3) 📊 The two factors that decide everything: **data size + bandwidth**

The module explicitly drills this point:

* **1 TB @ 100 Gbps ≈ ~2 minutes**
* **1 TB @ 100 Mbps ≈ ~30 hours**

Decision rule:

* If bandwidth is **good** → online options work (gcloud storage / STS / Datastream).
* If bandwidth is **poor** and data is **huge** → **Transfer Appliance**.

> 💡 **Exam Tip**
> If a scenario includes transfer time constraints or slow links, the correct answer is often chosen by this bandwidth logic.

---

## 4) 🧰 Toolbox Overview (what each tool is “for”)

| Need                             | Best tool                            | Why                                                |
| -------------------------------- | ------------------------------------ | -------------------------------------------------- |
| Ad-hoc copy to GCS               | **gcloud storage cp**                | Simple CLI transfer for small/medium datasets      |
| Large online transfer to GCS     | **Storage Transfer Service**         | Managed, efficient, supports scheduled transfers   |
| Massive offline migration        | **Transfer Appliance**               | Avoids slow networks, designed for huge datasets   |
| Continuous DB changes (CDC)      | **Datastream**                       | Near real-time replication from DB logs to GCS/BQ  |
| Full DB migration for apps       | **Database Migration Service (DMS)** | Migrates DB engines into Cloud SQL/AlloyDB/Spanner |
| Complex format/system migrations | **Dataflow templates**               | ETL patterns for non-relational/NoSQL + transforms |

> **Exam nuance:** In this module, **STS is the “large online” file mover**, and **Transfer Appliance is the “offline huge” mover**. The quizzes strongly reinforce that split.

---

## 5) 🌊 Datastream Deep Dive (CDC you must know)

### 5.1 What Datastream does

* Enables **continuous replication** from relational DBs into GCP.
* Captures **INSERT / UPDATE / DELETE** by reading the source database’s change logs (write-ahead style logs).
* Supports:

    * **Backfill** (historical snapshot) and/or **only new changes**
    * **Selective replication** (schema/table/column)

### 5.2 Where CDC data lands

* **Direct → BigQuery** (analytics)
* **→ Cloud Storage** (raw events) and optionally:

    * **Dataflow** for custom processing → then to BigQuery
    * event-driven patterns

### 5.3 Source log mechanisms (recognise names)

* Oracle → **LogMiner**
* MySQL → **Binary Log**
* PostgreSQL → **Logical decoding / WAL**
* SQL Server → **Transaction logs**

> 💡 **Exam Tip**
> If the question mentions any of these log systems, it is **screaming Datastream**.

---

## 6) 🧾 Datastream event message structure (quiz-critical)

Datastream events contain:

1. **Generic metadata**
   Context: source table, timestamps, etc.

2. **Payload** ✅ (**this is the actual data changes**)
   The changed row data in **key-value** format (column → value).

3. **Source-specific metadata**
   Extra origin context: database/schema/table, change type (INSERT/UPDATE/DELETE), system identifiers.

> 💡 **Exam Tip (direct quiz hit)**
> “Actual data changes in key-value format” → **Payload**.

---

## 7) 🔢 Unified data types (cross-DB consistency)

Datastream normalises numeric types across databases:

* Oracle `NUMBER`, MySQL `DECIMAL`, PostgreSQL `NUMERIC`, SQL Server `DECIMAL`
  → replicated as **decimal** (unified type)

When it lands:

* **Avro (GCS)** → decimal
* **JSON (GCS)** → number
* **BigQuery** → native **NUMERIC**

Why it matters:

* Consistent typing across heterogeneous sources
* Fewer surprises in downstream processing

---

## 8) 🧪 Lab recap — Datastream: PostgreSQL → BigQuery (what you must be able to explain)

### Flow (high-level)

1. Prepare **Cloud SQL for PostgreSQL**
2. Enable logical replication (publication + slot)
3. Create **Datastream connection profiles**
4. Create **stream** (source → destination)
5. Validate replication in **BigQuery**
6. Mutate source data and verify changes appear in BigQuery

### The “examable” configuration details

* Cloud SQL flag: `cloudsql.logical_decoding=on`
* Replication artifacts:

    * **Publication**
    * **Replication slot**
* Stream config:

    * Select schema (`test`)
    * BigQuery dataset location = region
    * Staleness limit set (lab uses **0 seconds**)
* Verify with `SELECT * ... ORDER BY id`

---

## 9) 🧠 Decision cheats (memorise)

### Pick the tool

* “Ad-hoc upload/copy to GCS” → **gcloud storage cp**
* “Large online transfer to GCS, supports schedules, S3/Azure/HDFS” → **Storage Transfer Service**
* “Huge dataset + limited bandwidth + offline shipping” → **Transfer Appliance**
* “Continuous replication / CDC from relational DB logs” → **Datastream**
* “Move entire DB for application migration” → **Database Migration Service**
* “Complex ETL for odd sources/formats” → **Dataflow templates**

### Pick the landing zone

* Raw files / landing zone → **Cloud Storage**
* Analytics destination → **BigQuery**
* App transactional destination → **Cloud SQL / AlloyDB / Spanner** (depends on app needs)

---

## 10) ✅ Micro-Checklist for the Exam

* Understand **replicate & migrate** stage purpose.
* Choose based on **data size + bandwidth** (the module’s key decision axis).
* Know what each tool does:

    * `gcloud storage cp` (ad-hoc)
    * **STS** (large online + scheduled)
    * **Transfer Appliance** (offline massive)
    * **Datastream** (CDC replication to GCS/BQ)
* Datastream internals:

    * Reads DB logs (LogMiner/binlog/WAL/logical decoding/txn logs)
    * Event structure: **metadata vs payload vs source-specific metadata**
    * Unified numeric type mapping
* Lab: explain replication slot/publication + validate changes in BigQuery.

---

## 11) 📝 Quiz mapping (what they’re testing)

1. “Actual changes key-value format” → **Payload**
2. Migration ease influenced by → **Data size + network bandwidth**
3. Very large offline migration → **Transfer Appliance**
4. Tool that uses `cp` ad-hoc to Cloud Storage → **gcloud storage command**
5. Large online transfer from on-prem/multicloud/HDFS to GCS with scheduling → **Storage Transfer Service**