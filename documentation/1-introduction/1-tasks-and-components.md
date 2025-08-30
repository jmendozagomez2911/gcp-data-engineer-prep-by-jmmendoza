# 📘✨  Data Engineering Tasks & Components (Google Cloud)

**Goal:** Understand *what* a data engineer does and *which* Google Cloud services to choose at each step of a pipeline.
**How to use this guide:** Read top-down. Skim the **Exam Tips** boxes. Copy the example commands/SQL when you practise.

---

## 1) 🧭 The Data Engineer’s Job (mental model)

A data engineer **designs, builds, and operates data pipelines** so the business can use data in **dashboards, reports, ML models, and apps**.

Typical responsibilities:

* **Ingest** data from many sources (batch & streaming).
* **Transform** it into clean, trustworthy, analytics-ready datasets.
* **Store/Serve** it in the right system (warehouse, lake, or operational store).
* **Govern** it (quality, security, lineage, metadata).
* **Productionise** pipelines (SLA/SLOs, monitoring, cost control).

### The four pipeline stages (you’ll see these everywhere)

1. **Replicate & Migrate** – get data *into* Google Cloud from other systems.
2. **Ingest** – land it so downstream tools can read it (becomes a **data source**).
3. **Transform** – clean/join/aggregate/enrich for specific use cases.
4. **Store** – place final, modeled data in a **data sink** for analysis/serving.

> 💡 **Exam Tip**
> Expect scenario questions like: *“Logs from on-prem apps must be analysed in near-real time.”* Think: **Pub/Sub → Dataflow → BigQuery** for streaming analytics, or **Transfer Service / Storage Transfer → Cloud Storage → BigQuery** for batch.

---

## 2) 🔌 Source vs Sink (don’t mix them up)

* **Data Source** = starting point of the journey (raw data).
  Common GCP sources at ingest:

    * **Cloud Storage** (files of any type; “data lake” landing zone).
    * **Pub/Sub** (asynchronous messaging / event ingestion).

* **Data Sink** = final resting place for **processed** data (ready to consume).
  Typical sinks:

    * **BigQuery** (analytics/OLAP warehouse).
    * **Bigtable** (very low-latency NoSQL for serving/operational analytics).

---


## 3) 🧩 Data Formats (know where they fit)

* **Unstructured** – binary/text without fixed schema (docs, images, audio, video).

    * Store in **Cloud Storage**.
    * Use **BigQuery Object Tables** to *reference* (not store) Cloud Storage objects for metadata-driven analytics over unstructured assets.

* **Semi-Structured** – self-describing formats (JSON, Avro, Parquet, ORC).

    * Land in **Cloud Storage** or stream directly via **Pub/Sub → Dataflow → BigQuery**.
    * **BigQuery natively supports** JSON, Avro, Parquet, ORC.
    * Nested/repeated fields are handled via **STRUCT** and **ARRAY** types.

* **Structured** – tabular data with a fixed schema (CSV, relational tables).

    * Load into **BigQuery** (data warehouse) or transactional stores like **Cloud SQL / AlloyDB / Spanner**.
    * Optimised for **OLAP (BigQuery)** vs **OLTP (Cloud SQL/AlloyDB/Spanner)** workloads.

---

👉 This way the distinction is crystal clear:

* **BigQuery = structured + semi-structured** (native support).
* **Unstructured = Cloud Storage** (with optional BigQuery Object Tables to analyse metadata).


> 💡 **Exam Tip**
> CSV is simple but not optimal at scale (no types, bigger size). **Parquet/ORC** → faster loads, lower query cost in BigQuery.

---


## 4) 🗄️ Storage & Databases on Google Cloud (selection guide)

Ahh, now I see what you want 👌 — keep the **table concise**, and then add a **section right after** explaining **vertical vs horizontal scaling** + exam tips (instead of cramming it inside the table).

Here’s the revised version:

---

## 4) 🗄️ Storage & Databases on Google Cloud (selection guide)

| Service                 | Type & When to Use                                                                                                                            | Highlights                                                                                                                                                                    |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Cloud Storage (GCS)** | Object store for **unstructured & semi-structured** data; data lake landing zone; archives; static website hosting (accessed by HTTP request) | Stores any binary object up to **5 TB**; strong consistency; range GET; lifecycle mgmt; **storage classes**: Standard / Nearline (≥30d) / Coldline (≥90d) / Archive (≥365d)   |
| **BigQuery**            | **Serverless, columnar data warehouse** for analytics (OLAP); structured + semi-structured data (JSON, Avro, Parquet, ORC)                    | SQL interface; separates storage/compute; scans TBs in seconds / PBs in minutes; built-in **ML**, **GIS**, **BI Engine**; **Object Tables** reference GCS unstructured assets |
| **Bigtable**            | **NoSQL wide-column** DB (schema-flexible tables grouped by column families) for petabyte-scale, low-latency operational data                 | Sub-10 ms reads/writes; ideal for **time-series, IoT, personalization, ML feature stores**; design row key carefully; not relational                                          |
| **Cloud SQL**           | Managed **MySQL / PostgreSQL / SQL Server** for traditional OLTP apps                                                                         | Easy lift-and-shift for legacy apps; **vertical scaling** (scale up VM size); regional only; good for SMB workloads                                                           |
| **AlloyDB**             | Fully managed **PostgreSQL-compatible** for high-performance OLTP + HTAP                                                                      | Up to **4× faster** than standard Postgres; ML-accelerated analytics; auto-scaling; designed for modern enterprise apps                                                       |
| **Spanner**             | **Horizontally scalable relational** DB with **global consistency**                                                                           | Only GCP service combining **SQL + ACID transactions + horizontal scale**; global distribution; mission-critical OLTP with very high throughput                               |
| **Firestore**           | Serverless **NoSQL document** store for app dev                                                                                               | Hierarchical docs/collections; serverless auto-scaling; ACID at document level; great for mobile/web apps needing flexible schemas                                            |


## 🔎 Vertical vs Horizontal Scaling in GCP Data Stores

### **Vertical Scaling (scale up a single instance)**

* Add more CPU, RAM, or disk to a single machine.
* Limited by the biggest VM size in that region/zone.
* Examples from the table:

  * **Cloud SQL** → MySQL/Postgres/SQL Server; classic vertical scaling.
  * **AlloyDB** → auto-scaling is smarter, but fundamentally still vertical per instance.
  * **Firestore** → auto-scales, but under the hood it’s managed in a way you can’t control; you don’t shard it manually.
  * **BigQuery (compute slots)** → technically scales horizontally under the hood, but *you* don’t manage it (serverless). From an exam POV → treat it as *elastic/automatic scaling*, not “you add nodes.”

---

### **Horizontal Scaling (add more nodes/servers; sharding/distribution)**

* Scale out linearly by distributing data + queries across multiple machines.
* Much harder technically because of **consistency problems**.
* Examples from the table:

  * **Spanner** → the **only relational DB** with horizontal scale (SQL + ACID).
  * **Bigtable** → wide-column NoSQL, shards data by row key across nodes; classic horizontal scaling.
  * **Firestore** → serverless NoSQL doc DB that auto-scales horizontally, but you don’t manage nodes.
  * **BigQuery** → storage and compute scale independently and horizontally, but managed by Google (serverless, not manual).


---

## 5) 🛶 Data Lake vs 🏛️ Data Warehouse

* **Data Lake (on GCS)** — raw, multi-format (un/semi/structured), cheap storage, flexible for **data science** and varied use cases.
* **Data Warehouse (BigQuery)** — curated, modeled data for **analytics & reporting**, fast SQL at scale, governance & performance features.

> 💡 **Modern pattern:** Land raw in **GCS (lake)** → curate and **ELT** into **BigQuery (warehouse)** → serve BI/ML.

---

## 6) 🚀 BigQuery Primer (what you *must* know)

### Core concepts

* **Hierarchy**: `project.dataset.table` (you’ll use this in SQL/CLI/API).
* **Access**: IAM at **dataset/table/view/column**. Need at least **read** on the object you query.
* **Table types**: Native tables, **external tables** (read in place from GCS), **views**, **materialized views**, and **object tables**.
* **Access methods**:Web UI Console SQL editor, **bq** (bigQuery) CLI, REST API (client libs).

### Ingestion (what you practised)

* Console load from CSV with **Auto Detect** schema.

* CLI **append** from GCS:

  ```bash
  bq load \
    --source_format=CSV \
    --autodetect \
    --noreplace \        # append if table exists
    nyctaxi.2018trips \
    gs://cloud-training/OCBL013/nyc_tlc_yellow_trips_2018_subset_2.csv
  ```

    * Use `--replace` to overwrite instead of append.

* **Create from query (CTAS)**:

  ```sql
  CREATE TABLE nyctaxi.january_trips AS
  SELECT * FROM nyctaxi.2018trips
  WHERE EXTRACT(MONTH FROM pickup_datetime) = 1;
  ```

### Querying example (top 5 most expensive trips)

```sql
SELECT *
FROM nyctaxi.2018trips
ORDER BY fare_amount DESC
LIMIT 5;
```

### 📉 Partitioning & Clustering (performance & cost)

* **Partition** by ingestion time or by a DATE/TIMESTAMP/INTEGER range column.

    * Always **filter on the partition column** to scan less and pay less.
* **Cluster** on up to 4 columns to improve predicate filtering and join performance.

### 💸 Cost control quick wins

* Preview schemas, **query only needed columns**, **use partition filters**, and cap with `LIMIT` during exploration.
* Prefer **Parquet/ORC** over CSV for efficient loads/queries.

> 💡 **Exam Tip**
> If a query is slow/expensive and filters by `event_date`, answer: **partition by date** and **cluster by common filter/join keys**.

---

## 7) 🧱 Transformation Patterns (and when to use them)

* **EL (Extract & Load)** – move raw into the lake/warehouse; transform later.
* **ELT (Extract, Load, Transform)** – **load first**, then transform *in BigQuery* with SQL (most common in modern analytics).
* **ETL (Extract, Transform, Load)** – transform before loading (e.g., compliance, heavy reshaping in **Dataflow/Dataproc**).

> 💡 **Choosing guide:**
>
> * Heavy SQL-friendly transformations → **ELT in BigQuery**.
> * Stream processing, windowing, exactly-once → **Dataflow** before load (ETL/ELT hybrid).
> * Spark/Hadoop skills or existing jobs → **Dataproc**.

---


## 8) 🗂️ Metadata, Governance, and Zones with **Dataplex**

* **Dataplex** centralises **discovery, governance, security policies, and lifecycle** across lakes & warehouses.
* ❌ Dataplex does **not store data** → it manages and governs data already in **BigQuery, Cloud Storage, Bigtable**, etc.
---

### 🔹 Typical **lake zones** (with governance applied)

* **Raw (Bronze)** — minimally processed, ingested as-is.

  * **Governance**: strict access controls; usually only data engineers/scientists.
  * Policies: schema validation, retention rules, sensitivity tags (e.g., PII flagged).
  * Metadata: catalogued as *raw*, often with limited discoverability.

* **Curated (Silver/Gold)** — cleaned, standardized, modeled for broader use.

  * **Governance**: broader access (analysts, BI users).
  * Policies: role-based permissions, data quality checks (freshness, completeness), compliance enforcement.
  * Metadata: enriched, discoverable in the catalog with business-friendly tags.

---

### 🔑 Governance capabilities in Dataplex

1. **Access & security** → IAM + fine-grained policies per lake/zone/table/object.
2. **Policy enforcement** → data retention, sensitivity classification, regulatory compliance.
3. **Metadata catalog** → unified search & discovery across GCS, BigQuery, etc.
4. **Data quality & lineage** → enforce rules (e.g., no null IDs), track data origins and transformations.

---

> 💡 **Exam Tip**
> Scenario mentions “break down data silos,” “unified governance,” or “discover data assets across GCS + BigQuery” → **Dataplex**.

---

## 9) 🔗 Data Sharing with **Analytics Hub**

* Publish **analytics-ready BigQuery datasets** and let others **subscribe**—**data stays in place** (no copying).
* Providers keep **control & monitoring** of usage; easy **internal/external** sharing; supports **monetisation** models.
* Great for building a **data ecosystem** (e.g., share with partners or lines of business).

> 💡 **Exam Tip**
> Keywords like “share externally,” “avoid data duplication,” “monitor usage,” “managed marketplace” → **Analytics Hub**.

---

## 10) 🧪 Hands-On Recap (from your lab)

1. **Create a dataset** `nyctaxi`.
2. **Load CSV** (Console): *Create table → Upload → CSV → Auto-detect → Table: `2018trips`*.
3. **Query** top fares:

   ```sql
   SELECT * FROM nyctaxi.2018trips ORDER BY fare_amount DESC LIMIT 5;
   ```
4. **Append** more data from **GCS** (CLI) with `bq load --noreplace`.
5. **Create January table** (CTAS) and **find longest trip**:

   ```sql
   SELECT * FROM nyctaxi.january_trips
   ORDER BY trip_distance DESC
   LIMIT 1;
   ```

---

## 11) 🧠 Quick Decision Trees (print-worthy)

**Choosing an ingestion path**

* Files batch → **Storage Transfer** or direct upload → **GCS** → **BigQuery load/external**.
* Events/streaming → **Pub/Sub** → **Dataflow** (or BigQuery streaming) → **BigQuery**.

**Choosing a storage system**

* Analytics/SQL at scale → **BigQuery**.
* Low-latency key/value/time-series → **Bigtable**.
* Global relational with transactions → **Spanner**.
* Traditional relational app → **Cloud SQL** (or **AlloyDB** if you need more performance).
* App docs, serverless → **Firestore**.
* Files/objects, cheap & durable → **Cloud Storage**.

**Choosing transform pattern**

* Mostly SQL; want speed to insights → **ELT in BigQuery**.
* Complex streaming / exactly-once → **Dataflow**.
* Existing Spark codebase → **Dataproc**.

---

## 12) ✅ Micro-Checklist for the Exam

* ✅ Define **source vs sink** and map GCP services to each pipeline stage.
* ✅ Identify correct store: **BigQuery vs Bigtable vs Spanner vs Cloud SQL vs Firestore vs GCS**.
* ✅ Recall **Cloud Storage classes** and when to use each.
* ✅ Understand **EL / ETL / ELT** and pick based on scenario.
* ✅ BigQuery must-knows: `project.dataset.table`, **IAM scopes**, **partitioning & clustering**, **CTAS**, **bq load** flags.
* ✅ Governance & sharing keywords: **Dataplex** (unified metadata/governance), **Analytics Hub** (in-place sharing/monetisation).

---

## 13) 🎯 Practice Prompts (teach-yourself checks)

1. *You need to share a curated dataset with a partner without copying data and want to monitor their usage. What service?* → **Analytics Hub**.
2. *IoT readings at 100K writes/sec, sub-10ms reads by row key—what store?* → **Bigtable**.
3. *Legacy MySQL app migrates with minimal change—what service?* → **Cloud SQL (MySQL)**.
4. *You must query CSVs in GCS immediately, no load step—what to create?* → **External table in BigQuery**.
5. *Query cost too high scanning months of data by `event_date`—what to do?* → **Partition by date** and **filter on it**; consider **clustering**.

---

### 👩‍🏫 Final thought (teacher’s nudge)

If you can **explain why** a workload belongs in **BigQuery** vs **Bigtable** vs **Spanner**, and **show** how to load/query/partition in BigQuery, you will ace a big chunk of the questions in this module’s scope. Keep these patterns in your head and map scenarios to them quickly.
