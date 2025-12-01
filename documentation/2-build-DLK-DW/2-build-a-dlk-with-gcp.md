# 📘✨ **README — Module 2: Build a Lakehouse with Cloud Storage, Open Formats & BigQuery**

**Goal:** Be able to name every moving part of a **Google Cloud lakehouse**, explain how they fit, and build/join data across **Cloud Storage + open tables (Apache Iceberg) + BigQuery + AlloyDB**—without copying data.

**Use this like:** 1) foundation → 2) Iceberg (open table layer) → 3) BigQuery/BigLake (engine) → 4) AlloyDB (operational) → 5) Federated queries → 6) real-world pattern → 7) lab recap → 8) exam cheats.

---

## 1) 🧱 Foundation: Storage choices in a lakehouse

* **Cloud Storage (GCS)** = primary, low-cost **object store** for *all* raw files (structured, semi-structured, unstructured, even multimedia).

    * “Store now, decide later” (schema-on-read) and keep **open formats** (Parquet/ORC/Avro/CSV/JSON, images/video).
    * Multi-cloud ready: lakehouse can reference data in other clouds’ object stores too.

* Why this matters for Cymbal (retail example): same bucket can hold **sales CSV/Parquet**, **clickstream JSON**, **reviews/images/video**—the full customer journey.

> Exam tells: “primary, cost-effective object storage for a lakehouse?” → **Cloud Storage**.

---

## 2) 🧊 Apache Iceberg: the open table format layer

**Problem:** Querying raw files means directory scans and brittle partitions.
**Answer:** **Iceberg** adds **metadata + table semantics** on top of files in GCS (no data move).

What Iceberg gives you:

* **Schema evolution** (add/rename columns without rewrite).
* **Hidden partitioning** (avoid partition-key mistakes; engine prunes automatically).
* **Time travel** (query a table as of a snapshot).
* **Atomic transactions** (consistent reads/writes over many files).

> Mental model: Iceberg = “catalog + manifest + snapshots” over your Parquet/ORC files.

---

## 3) 🐳 BigQuery + BigLake: the central processing engine

* **BigQuery** is the **SQL brain** of the lakehouse:

    * Query **native BigQuery tables** (for hot/curated data) **and** **Iceberg tables in GCS** via **BigLake**—**without copying**.
    * Keep a single interface (standard SQL, BI Engine, governance).

* **BigLake** binds it together:

    * Enforces **row/column-level security** and **policy tags** over external files.
    * Works across **GCS / S3 / ADLS** (multi-cloud).
    * Lets BigQuery push down filters using Iceberg metadata for performance.

> Rule of thumb: **Hot KPI datasets** → BigQuery storage; **wide/raw/cheaper** → Iceberg on GCS; query both together.

---

## 4) ⚙️ AlloyDB (operational data)

* **AlloyDB for PostgreSQL** = high-throughput, low-latency **OLTP** for live operations:

    * **Order processing**, **inventory**, **logins**—lots of small reads/writes with strong consistency.
    * PostgreSQL-compatible, highly available, and much faster than stock Postgres.

> Exam tells: “Which store for real-time orders/inventory?” → **AlloyDB**, *not* BigQuery.

---

## 5) 🔗 Federated queries: join operational + analytical in place

You can query **AlloyDB** live from BigQuery using **federated queries**—no ETL:

1. Create a **BigQuery external connection** to AlloyDB.
2. Grant its **connection service account**:

    * `roles/cloudalloydb.client` and `roles/bigquery.connectionUser`.
3. Use **`EXTERNAL_QUERY`** to pull rows from AlloyDB inside your SQL:

```sql
-- Read a sample directly from AlloyDB
SELECT *
FROM EXTERNAL_QUERY(
  "PROJECT_ID.REGION.AlloyDB-weblog",
  "SELECT customer_id, CAST(log_id AS VARCHAR(200)) AS log_id, timestamp, url FROM web_log LIMIT 100"
);
```

Join with BigQuery native/Iceberg tables:

```sql
WITH log AS (
  SELECT customer_id, log_id, timestamp, url
  FROM EXTERNAL_QUERY(
    "PROJECT_ID.REGION.AlloyDB-weblog",
    "SELECT customer_id, CAST(log_id AS VARCHAR(200)) AS log_id, timestamp, url FROM web_log LIMIT 100"
  )
)
SELECT  l.customer_id, l.timestamp, l.url, c.*
FROM customers.customer_details AS c
JOIN log AS l
  ON c.id = l.customer_id
ORDER BY c.id
LIMIT 100;
```

> Exam tells: “Unify live operational data with historical analytics **without copying**” → **BigQuery federated queries**.

---

## 6) 🧩 Real-world pattern (Cymbal)

* **Marketing ROI**: join **campaign cohorts** (BigQuery native) + **web behavior** (AlloyDB logs via federation) + **orders** (Iceberg on GCS).
* **Supply chain**: blend **inventory (AlloyDB)** + **demand signals (Iceberg clickstream)** → replenishment models; surface **BI** in BigQuery.

**Outcome:** one governed platform for BI + DS/ML without data silos or duplication.

---

## 7) 🧪 Lab recap — **Federated query with BigQuery & AlloyDB**

* Enable **BigQuery Connection API**.
* **Create connection** (Studio → Add data → Databases → AlloyDB → *BigQuery Federation*).

    * `Connection ID: AlloyDB-weblog`, set instance path to provided AlloyDB instance.
* **Grant IAM** to the connection’s **service account**:

    * `Cloud AlloyDB Client`, `BigQuery Connection User`.
* **Query**:

    * Use `EXTERNAL_QUERY` to pull AlloyDB rows.
    * Join with `customers.customer_details` (BigQuery) as shown above.

---

## 8) 🧠 Quick decision cheats (exam)

* **Primary object store for lakehouse** → **Cloud Storage**.
* **Open table semantics over files** (schema evolution, time travel, atomicity) → **Apache Iceberg**.
* **Central SQL engine across files + tables + external DBs** → **BigQuery** (+ **BigLake** for governed access to files).
* **High-TPS operational workloads** → **AlloyDB**.
* **Join AlloyDB with BQ/Iceberg without ETL** → **Federated queries / `EXTERNAL_QUERY`**.
* **Need governance & security over files** (row/column/policy tags) → **BigLake** (often with **Dataplex** for catalog/governance).

---

### 👩‍🏫 Teacher’s nudge

Anchor this triangle in your head: **GCS (files) + Iceberg (open tables) + BigQuery/BigLake (SQL + governance)**, with **AlloyDB** sitting beside it for OLTP and **federated queries** as the live bridge. If a scenario mentions **“one query across raw files + warehouse + operational DB”**, your answer is **BigQuery + BigLake + EXTERNAL_QUERY**.
