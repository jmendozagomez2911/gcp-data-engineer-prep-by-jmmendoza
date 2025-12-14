# 🤖📦 **README — Module 01: When to choose batch data pipelines**

**Goal:** Know *exactly* when a **batch data pipeline** is the right pattern, what its core components are, which GCP services map to each stage, and which design ideas the **Professional Data Engineer** exam will test.

**Read me like this:**

1. What batch pipelines are → 2) When to choose batch vs streaming → 3) Components + GCP mapping → 4) Batch processing vs pipeline → 5) Core features (throughput, latency, cost) → 6) Cymbal challenges → 7) Ingestion patterns (Cloud Storage hub) → 8) Exam-style patterns + quiz logic → 9) Micro-checklist.

---

## 1) 🧠 Mental model: what is a *batch data pipeline*?

**Definition (exam level):**

> A **batch data pipeline** is a *sequence of processes* that **ingests → transforms → sinks** *large, finite datasets* (“batches”) at **scheduled intervals**, optimized for **throughput and efficiency**, not for low latency.

Contrast with streaming:

* **Batch**: works on **bounded** data (e.g. “all transactions for 2025-01-01”), processed in one or more scheduled runs.
* **Streaming**: works on **unbounded**, continuously arriving data (events in real time).

> **Exam tip:** Words like *“end of day”, “daily after business hours”, “monthly report”, “five years of historical data”* → very strong batch signal.

---

## 2) 📌 When is batch the right pattern? (Use cases)

Typical **batch-friendly** scenarios:

* **Scheduled reports & analytics on historical data**

    * Daily/weekly/monthly KPIs, trend analysis, financial reports.
* **Massive datasets that need heavy transformations**

    * Cleaning, aggregation, joins, business logic over *all* the data.
* **Data warehouse loading / ELT**

    * Periodic loads from OLTP systems, third-party data, logs → **BigQuery**.
* **Bulk data movement**

    * Large volumes from on-prem → cloud or between systems.
* **Backups / archival / DR**

    * Large periodic snapshots of data for recovery/compliance.

> **Exam pattern:**
> “Process **all of the day’s transactions at once** after close of business; **real-time is not required**; must be **throughput-optimised and cost-effective**” → **Batch processing**.

---

## 3) 🏗️ Components of a batch pipeline + GCP mapping

Keep this pipeline shape in your head and map each part to GCP services.

### 3.1 Data sources

* Operational DBs, CSV, JSON, log files, third-party APIs, other clouds.
* Often heterogeneous (schemas, formats, frequencies).

### 3.2 Data ingestion (→ landing zone)

* **What it is:** Move raw data from sources to a **central staging area**.
* **On GCP:**

    * **Cloud Storage** bucket as **landing / staging** zone.
    * In some cases: direct ingestion into **BigQuery** or from other clouds.

> **Architectural best practice:**
> **Land raw data in durable storage before transforming**. This **decouples ingestion from processing** and lets you re-run failed jobs from the raw data.

➡ **Quiz #1 principle:**
Correct answer: *“Because it decouples ingestion from processing, allowing the transformation job to be re-run from the raw source data if it fails.”*

---

### 3.3 Data transformation

* Clean, validate, enrich, join, aggregate, map to canonical schemas, apply business rules.
* **On GCP:**

    * **Dataflow (Apache Beam)** — unified model for **batch & streaming**.
    * **Dataproc Serverless for Apache Spark** — run Spark code without managing clusters.

> **Exam tip:** Team with existing **Spark** jobs and want serverless → **Dataproc Serverless (for Spark)**, **minimal code changes**.
> (Quiz #10 principle.)

---

### 3.4 Data sink (final storage)

* Where transformed data ends up for analytics / downstream use.

Common GCP sinks:

* **BigQuery** → analytical DWH for interactive SQL over massive datasets.
* **Cloud Storage** data lake (often with table formats like **Iceberg**, etc.).
* Other analytical stores depending on use case.

---

### 3.5 Downstream uses (not “pipeline”, but very examinable)

Examples (Cymbal Superstore):

* **Financial reporting** in BigQuery.
* **BI dashboards** on historical data.
* **ML models** using years of cleaned transaction data to forecast sales.

---

### 3.6 Orchestration & monitoring (wraps the whole pipeline)

* **Orchestration:** order, dependencies, schedules, retries.

    * On GCP: **Cloud Composer** (Airflow), **Workflows**, **Cloud Scheduler**.
* **Monitoring & observability:** health, errors, performance, SLAs.

    * On GCP: **Cloud Logging**, **Cloud Monitoring**, alerts, dashboards.

> **Quiz #8 principle:**
> “Failure discovered hours later, hard to find errors” → challenge is **Reliability & Observability**, solved by **centralized logging and metrics-based monitoring**.

---

## 4) 🏪 Cymbal Superstore: concrete mapping to GCP

Learn this story as a template:

1. **Sources:** CSV + JSON billing data from many systems.
2. **Ingestion:** Automated landing into **Cloud Storage** (central staging).
3. **Transformation:**

    * Use **Dataflow for Apache Beam** *or* **Serverless for Apache Spark**
    * Read raw data from Cloud Storage → clean, validate, standardize.
4. **Sink:** Write cleansed, structured data to **BigQuery** (enterprise warehouse).
5. **Downstream:**

    * Financial reports, dashboards, ML on historical sales.
6. **Orchestration & monitoring:**

    * **Cloud Composer** schedules jobs.
    * Logging + monitoring to ensure reliability and data quality.

> **Exam tip:** If the case mentions **“millions of daily transactions”, “financial reconciliation”**, and **no real-time need**, plus GCP services like **Cloud Storage + Dataflow/Dataproc + BigQuery**, they are describing **exactly this pattern**.

---

## 5) 🧬 Batch *processing* vs batch *data pipelines*

You must be able to distinguish the terms:

| Term                      | Focus                                                                                                                                 |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| **Batch data processing** | The **method**: collect data in a **batch** and process it in a scheduled run.                                                        |
| **Batch data pipeline**   | The **end-to-end system** that **implements** batch processing (sources → ingestion → transform → sink → orchestration & monitoring). |

> In casual speech they’re mixed, but exam questions may talk about **“batch processing features”** vs **“pipeline architecture”**.

---

## 6) ⚙️ Core features of batch data processing (what exam loves)

Batch processing normally implies these **four properties**:

1. **Scheduled & automated**

    * Jobs run **on a schedule** (nightly, hourly, monthly) with **no manual intervention**.
2. **High throughput**

    * Optimised for **processing huge volumes** efficiently (terabytes, years of history).
3. **Latency-tolerant**

    * Accepts **higher latency** (e.g. results ready next morning) in exchange for efficiency.
    * Ideal for **bounded historical workloads** (e.g. 5 years of sales).
4. **Resource optimisation (burst usage)**

    * Compute can **scale up during the job** then **scale down or shut off**.
    * Avoids paying for always-on infrastructure.

> **Quiz #2 principle:**
> Historical 5-year dataset for model training → key idea is: **batch is designed for very large, bounded datasets**.

> **Quiz #4 principle:**
> “Complex validations across **entire day’s data at once**” → relies on **operating on a complete, bounded dataset**.

> **Quiz #6 principle:**
> CFO wants to cut costs; job runs 4h/day → serverless batch **only charges during execution** (resource optimisation).

---

## 7) 🛰️ Serverless & cost: why the business cares

When the exam says **“fully serverless”**, think like this:

* You **don’t manage infrastructure** (no cluster sizing, patching, OS updates).
* Platform handles **provisioning, scaling, tear-down**.
* You pay **only while jobs run**, not 24/7.

> **Quiz #5 principle:**
> Main business value: **reduces total cost of ownership by shifting operational overhead** to the cloud provider.

> **Quiz #6 principle (again):**
> On-prem cluster running 24/7 vs serverless that runs 4h/day → **direct cost saving** by paying only for active job time.

And for teams with existing Spark:

> **Quiz #10 principle:**
> Most logical move is **“adopt managed/serverless that runs existing Spark code with minimal changes”** → on GCP, think **Dataproc Serverless for Spark**.

---

## 8) 📥 Initial ingestion patterns (Cloud Storage as the hub)

### 8.1 Cloud Storage = central staging layer

Key architectural idea:

* Land **all raw batch files (CSVs, JSON, logs, etc.) into Cloud Storage**.
* Treat this as your **single source of truth** for raw data.

**Why it matters:**

* **Decouples** the data source systems from the processing engine.
* Allows **re-runs** if processing fails (no need to re-pull from source).
* Enables multiple downstream consumers (Dataflow, Dataproc, BigQuery, ML).

> **Quiz #1 principle (repeated):**
> The primary reason this adds resilience is **decoupling ingestion from processing**.

---

### 8.2 Programmatic ingestion (Python example – what you must “recognise”)

```python
from google.cloud import storage

def upload_blob(bucket_name, source_file_name, destination_blob_name):
    storage_client = storage.Client()
    bucket = storage_client.bucket(bucket_name)
    blob = bucket.blob(destination_blob_name)
    blob.upload_from_filename(source_file_name)
    print(f"File {source_file_name} uploaded to {destination_blob_name} in bucket {bucket_name}.")
```

You don’t need to memorise line-by-line code, but you must understand:

* **Pattern:** local/system file → **Cloud Storage bucket**.
* Afterwards, **Dataflow / Dataproc Serverless** can read from that bucket.

> **Concept tested:** “Data is **programmatically landed** in a bucket, then batch jobs pick it up” → standard GCP pipeline pattern.

---

### 8.3 Multi-cloud support (high level)

* GCP can **ingest or process data residing in other clouds** without always copying everything first.
* For the exam, the key idea is: GCP can operate in **multi-cloud** scenarios and still use **Cloud Storage + Dataflow/Dataproc/BigQuery** as processing platform.

---

## 9) 🧩 Typical challenges of batch pipelines (Cymbal’s problems)

Know these four buckets; exam scenarios are written around them.

1. **Data Volume & Scalability**

    * Rapid data growth overwhelms legacy systems.
    * Need **auto-scaling** pipelines that cope with spikes (e.g. triple volume during sales events).
    * **Quiz #9 principle:** daily volume triples and fixed pipeline fails → challenge is **Data Volume & Scalability**.

2. **Data Quality & Consistency**

    * Many sources, formats, and schema variations.
    * Need cleaning, validation, standardisation to avoid **incorrect financial reports**.

3. **Complexity & Maintainability**

    * Adding more sources and business logic → messy scripts, hard to debug and evolve.
    * Need **well-designed pipelines** with clear stages & orchestration.

4. **Reliability & Observability**

    * Job failures delay reports; errors hard to find.
    * Need **retries, alerts, centralized logging, metrics**.
    * **Quiz #8 principle:** reliability/observability problem → solved by **logging + monitoring**, not by “more transformation frameworks”.

> **Quiz #7 principle:**
> When nightly reconciliation fails across multiple sources, the robust long-term solution is:
> **“Design an automated, end-to-end batch data pipeline that orchestrates collection, cleansing, validation on a nightly schedule.”**

---

## 10) 🧠 Future-proofing: batch now, streaming later

A classic exam pattern:

* Requirement **today**: batch.
* Possible requirement **tomorrow**: streaming (near real-time).

Best design choice:

> **Select a programming model that works for both batch and streaming so you can reuse business logic.**

On GCP this screams **Apache Beam (Dataflow)**.

➡ **Quiz #3 principle:** correct option is *“Select a programming model that is unified for both batch and streaming.”*

---

## 11) 📝 Exam-style logic behind the module quiz (quick mapping)

You don’t need the letters, just the *idea*:

1. **Landing raw → resilient**
   ⇒ Decouple ingestion from processing; re-run from raw.
2. **5 years historical data**
   ⇒ Batch is ideal for **large, bounded** datasets.
3. **Future streaming**
   ⇒ Pick **unified batch+streaming model** (e.g. Beam).
4. **Full-day financial validation**
   ⇒ Requires **complete, bounded dataset**.
5. **Fully serverless business benefit**
   ⇒ Lower TCO by shifting **ops overhead** to cloud provider.
6. **CFO & 4h batch on 24/7 system**
   ⇒ **Resource optimisation**: pay only when job runs.
7. **Nightly reconciliation failures**
   ⇒ Build **automated end-to-end batch pipeline**, not ad-hoc scripts.
8. **Failure found late, hard to debug**
   ⇒ **Reliability & Observability** → logging + monitoring tools.
9. **Volume triples, fixed resources fail**
   ⇒ **Data Volume & Scalability** challenge.
10. **Existing Spark team wants serverless**
    ⇒ Use **managed/serverless service that runs Spark with minimal changes** (Dataproc Serverless).

---

## ✅ Micro-Checklist (Module 1 cram)

Before moving on, make sure you can:

* Define a **batch data pipeline** and distinguish it from **streaming**.
* List **key use cases** where batch is clearly better (historical reports, huge bounded datasets, nightly reconciliation, model training).
* Draw the **pipeline stages** and map them to **GCP services**:

    * Sources → **Cloud Storage** (landing) → **Dataflow / Dataproc Serverless** (transform) → **BigQuery / GCS** (sink) → Composer / Logging & Monitoring.
* Explain the **4 core features** of batch processing:

    * Scheduled & automated, high throughput, latency-tolerant, resource-optimised.
* Argue why **landing raw data in Cloud Storage** makes the pipeline more **resilient and re-runnable**.
* Recognise **Cymbal’s four challenge categories**: volume, quality, complexity, reliability/observability.
* Explain the value of **serverless** for cost (no 24/7 clusters; pay-per-use).
* Justify choosing a **unified programming model** (Beam) to future-proof for streaming.

---

Si quieres, en el siguiente mensaje me puedes pasar **el siguiente módulo** y sigo construyendo READMEs así, uno por módulo, para que tengas un “libro de notas” listo para repasar antes del examen.
