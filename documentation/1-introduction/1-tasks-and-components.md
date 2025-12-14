# 📘✨ Data Engineering Tasks & Components (Google Cloud)

**Goal:** Understand *what* a data engineer does, the **4 pipeline stages**, and **which Google Cloud services** typically fit each stage (with exam-grade mental models).

**How to use this guide:** Read top-down. Focus on the **Exam Tips** and the **Decision Trees**. Practise the CLI/SQL snippets.

---

## 1) 🧭 The Data Engineer’s Job (mental model)

A data engineer **builds and operates data pipelines** so data can power **dashboards, reports, ML models, and apps**. The job is not just “moving data”: it’s making data **usable**, **accurate**, and **production-ready**.

What that includes:

* **Make raw data usable** (clean, validate, standardise).
* **Add value via transformations** (business logic, joins, enrichment).
* **Data management** (currency/freshness, accuracy, governance).
* **Production operations** (automation, monitoring, cost control, reliability).

### The 4 pipeline stages (course framing)

1. **Replicate & Migrate** – bring data into Google Cloud from internal/external systems.
2. **Ingest** – land data so it becomes a **data source** for downstream tools.
3. **Transform** – modify/join/aggregate to match downstream analytics requirements.
4. **Store** – deposit final, ready-to-consume data in a **data sink**.

> 💡 **Exam Tip**
> “Where does the data become available downstream?” → **Ingest stage**.
> “Where is the final, analytics-ready data stored?” → **Store stage** (sink).

---

## 2) 🔌 Data Source vs Data Sink (don’t mix them up)

* **Data Source** = the *starting point* (raw or newly landed data that downstream tools will read).

  * Common GCP “ingest-phase” sources:

    * **Cloud Storage** (landing zone / data lake for files)
    * **Pub/Sub** (asynchronous messaging for event ingestion)

* **Data Sink** = the *final stop* where **processed** data lives for analysis & decision-making.

  * Typical sinks:

    * **BigQuery** (serverless analytics warehouse)
    * **Bigtable** (low-latency NoSQL for operational/serving use cases)

> 💡 **Exam Tip**
> If they describe “final stop / reservoir at the end of the river” → they mean **sink**.

---

## 3) 🧩 Data Formats (what goes where)

### A) **Unstructured**

Docs, images, audio, video — non-tabular bytes.

* Best home: **Cloud Storage**
* BigQuery angle:

  * **BigQuery Object Tables** can represent/track objects (metadata + referencing), useful for analytics around assets.

### B) **Semi-structured**

JSON, Avro, Parquet, ORC.

* Land in **Cloud Storage** or load into **BigQuery**
* BigQuery supports nested structures via **STRUCT** and **ARRAY**
* Efficient formats:

  * **Parquet/ORC** → columnar, efficient scanning/cost
  * Avro also common for loads (schema embedded)

### C) **Structured**

CSV, relational tables.

* Analytics: **BigQuery**
* Transactional: **Cloud SQL / AlloyDB / Spanner** (OLTP-style)

> 💡 **Exam Tip**
> CSV is simple, but at scale it’s usually worse than columnar formats (types + size + scan cost). Prefer **Parquet/ORC** when possible.

---

## 4) 🗄️ Storage & Databases on Google Cloud (selection guide)

| Service                 | Best for                                                 | Exam highlights                                                                                                                                                     |
| ----------------------- | -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Cloud Storage (GCS)** | Un/semi-structured objects; landing zone; data lake      | Objects accessed via **HTTP**; supports **range GET**; object key = name; object size up to **5 TB**; classes **Standard / Nearline / Coldline / Archive**          |
| **BigQuery**            | Serverless OLAP analytics warehouse                      | Built-in **ML/GIS/BI**; very large scans; access via **Console SQL**, **bq CLI**, **REST API**; table ref `project.dataset.table`; IAM at dataset/table/view/column |
| **Bigtable**            | Low-latency wide-column NoSQL                            | **Key-value lookup**, sub-10ms latency; time-series/IoT/features/personalisation; row-key design matters                                                            |
| **Cloud SQL**           | Managed relational (MySQL/Postgres/SQL Server)           | Lift-and-shift OLTP; typically **vertical scaling**                                                                                                                 |
| **AlloyDB**             | High-performance Postgres-compatible OLTP/HTAP           | “Managed Postgres but faster” positioning; enterprise OLTP choice                                                                                                   |
| **Spanner**             | Horizontally scalable relational with strong consistency | SQL + ACID + horizontal scale + global consistency                                                                                                                  |
| **Firestore**           | Serverless NoSQL document DB                             | Auto-scaling; app dev; document/collection model                                                                                                                    |

---

## 5) 🛶 Data Lake vs 🏛️ Data Warehouse (the exam definition)

* **Data Lake (GCS)**
  Stores **raw** data in **many formats** (un/semi/structured). Flexible for DS, apps, exploration.

* **Data Warehouse (BigQuery)**
  Stores **processed/structured** and often **aggregated** data for **analytics & reporting**.

> 💡 **Modern pattern (very testable):**
> Land raw in **GCS** → transform/curate → load into **BigQuery**.

---

## 6) 🚀 BigQuery Primer (must-know facts)

### Core concepts

* Naming: `project.dataset.table`
* Datasets are **scoped to a project**
* Access control: **IAM** at **dataset/table/view/column**
* To query a table/view: need **at least read permission**

### Access paths

* Cloud Console SQL editor
* `bq` CLI (Cloud SDK)
* REST API + client libraries

---

## 7) 🧱 Transformation patterns (recognise in scenarios)

The transcript names these explicitly:

* **EL** (Extract & Load)
* **ELT** (Extract, Load, Transform) — common with BigQuery-centric analytics
* **ETL** (Extract, Transform, Load) — transform before loading (heavy reshaping/compliance)

> 💡 **Exam Tip**
> If the question hints “reuse logic for batch and streaming later”, pick **Dataflow/Beam** model later in the course.
> If it hints “existing Spark jobs, want serverless”, later you’ll use **Dataproc Serverless for Spark**.

---

## 8) 🗂️ Metadata & Governance with Dataplex

**Dataplex** = discover + manage + govern distributed data across GCS/BigQuery/etc.

Key promises from the transcript:

* Break down **data silos**
* Centralise **security & governance**
* Enable **distributed ownership**
* Improve **search & discovery** by business context
* Standardise metadata, policies, classification, lifecycle

### Zones (common pattern)

* **Raw zone** → mostly data engineers/scientists
* **Curated zone** → broader consumption (analysts, BI users)

> 💡 **Exam Tip**
> “Centrally discover/govern data across lakes + warehouses” → **Dataplex**.
> Dataplex **does not store** your data; it governs what’s already in GCS/BigQuery/etc.

---

## 9) 🔗 Data Sharing with Analytics Hub

**Analytics Hub** solves “sharing data is hard”, especially **outside** your org.

What it gives (from transcript):

* Publish + subscribe to **analytics-ready datasets**
* **Share in place** (no copying)
* Providers can **control and monitor usage**
* Self-service access to trusted datasets (including Google-provided)
* Enables **monetisation** without building the monetisation infrastructure

> 💡 **Exam Tip**
> Keywords: “share externally”, “in place”, “monitor usage”, “data ecosystem”, “monetise” → **Analytics Hub**.

---

## 10) 🧪 Lab recap (BigQuery loading essentials)

What you practised:

1. Create dataset `nyctaxi`
2. Load local CSV via Console (**Auto Detect**) into `nyctaxi.2018trips`
3. Query top fares:

```sql
SELECT * FROM nyctaxi.2018trips
ORDER BY fare_amount DESC
LIMIT 5;
```

4. Append more data from GCS with CLI (`--noreplace` means append):

```bash
bq load \
  --source_format=CSV \
  --autodetect \
  --noreplace \
  nyctaxi.2018trips \
  gs://cloud-training/OCBL013/nyc_tlc_yellow_trips_2018_subset_2.csv
```

5. Create a derived table with CTAS (DDL) for January:

```sql
CREATE TABLE nyctaxi.january_trips AS
SELECT *
FROM nyctaxi.2018trips
WHERE EXTRACT(MONTH FROM pickup_datetime) = 1;
```

---

## 11) 🧠 Quick decision trees (exam speed)

### Choosing ingestion

* **Files (batch)** → land in **GCS** → load/external in **BigQuery** (or transform first)
* **Events/streaming** → **Pub/Sub** → processing engine → sink

### Choosing storage

* Analytics warehouse → **BigQuery**
* Low-latency lookup/time-series/features → **Bigtable**
* Global relational ACID + horizontal scale → **Spanner**
* Traditional relational app DB → **Cloud SQL / AlloyDB**
* Serverless document DB for apps → **Firestore**
* Object/files/landing zone → **Cloud Storage**

---

## 12) ✅ Micro-Checklist for the Exam

* ✅ Data engineer’s role: pipelines → usable data → production operations
* ✅ 4 stages: replicate/migrate → ingest → transform → store
* ✅ Define **source vs sink** (Cloud Storage/PubSub vs BigQuery/Bigtable)
* ✅ Unstructured vs structured vs semi-structured placement
* ✅ GCS facts: HTTP access, range GET, max object size **5 TB**, storage classes
* ✅ BigQuery facts: `project.dataset.table`, datasets scoped to project, IAM levels
* ✅ Governance keywords → **Dataplex**
* ✅ Sharing/monetisation keywords → **Analytics Hub**

---

## 13) 🧪 Quiz mapping (what they’re really testing)

1. Primary function of data engineer → **build & maintain pipelines**
2. Unstructured (images/videos) → **Cloud Storage**
3. Lake vs warehouse → **raw vs processed/organised**
4. Analytics Hub → **secure controlled sharing inside/outside org**
5. Modify for downstream requirements → **Transform stage**

---

If you want, paste your **next module’s draft** (or just the transcripts) and I’ll keep updating them in the same style, making sure each README stays **deduped + exam-oriented**.
