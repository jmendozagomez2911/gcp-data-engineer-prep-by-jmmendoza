# 📘✨ **README — Module 1: Introduction to Modern Data Engineering on Google Cloud**

**Goal:** Be crystal-clear on **data lakes vs data warehouses vs data lakehouses**, and how Google Cloud maps to each.
**You’ll use this to:** pick the right architecture on the exam and in real projects.

---

## 1) 🧭 Mental model (in one minute)

* **Data Lake** = big, cheap **object storage** of **raw** data (schema-on-read). Great for AI/ML + exploration.
  → On GCP: **Cloud Storage** (+ open file formats like Parquet/ORC/Avro/JSON, images, video, logs).

* **Data Warehouse** = curated, modeled **structured** data (schema-on-write) for **fast BI/SQL**.
  → On GCP: **BigQuery** (serverless, ANSI SQL, partitioning/clustering, BI Engine).

* **Data Lakehouse** = lake **storage economics** + warehouse-grade **governance/performance** on one platform.
  → On GCP: **BigQuery + BigLake** (governance & fine-grained security over data in GCS/S3/ADLS), usually managed with **Dataplex** (catalog/governance).

> 🧪 Cymbal (e-commerce) example
>
> * Lake: clickstream JSON, images, reviews.
> * Warehouse: sales facts + dimensions for Finance.
> * Lakehouse: correlate **reviews sentiment** (lake) with **sales** (warehouse) in one governed layer.

---

## 2) 🏞️ Data Lake — when & why

**Traits**

* Stores **all types**: structured, semi-structured, unstructured.
* **Schema-on-read** (you decide structure when you query).
* Ingest is **fast/cheap**; can scale to **exabytes**.

**Pros** ✅
Flexibility • low cost • rapid ingest • ideal for **ML** and data science.

**Cons** ❌
Risk of **data swamp** without governance • more **wrangling** before analysis • can complicate **security/compliance**.

**GCP pieces**

* **Cloud Storage** buckets (+ lifecycle classes).
* Open formats: **Parquet/ORC/Avro/CSV/JSON**.
* Optional: **Dataplex** to catalog/govern, **BigQuery external tables/BigLake** to query in place.

---

## 3) 🏛️ Data Warehouse — when & why

**Traits**

* **Modeled & cleaned** structured data for **fast SQL analytics** (dashboards, ad-hoc BI).
* **Schema-on-write**; strong governance & performance.

**Pros** ✅
Interactive speed • consistent KPIs • fine-grained access control • easy BI.

**Cons** ❌
Less flexible for new/unstructured data; modeling work up front.

**GCP piece**

* **BigQuery** (serverless DW; partitions, clustering, materialized views, BI Engine, built-in ML/GIS).

---

## 4) 🐟+🏛️ Lakehouse — best of both

**What it is**
A **metadata & governance layer** over **open files** in **low-cost object storage**, **queried like warehouse tables**.

**On Google Cloud**

* **BigLake**: lets BigQuery enforce **row/column-level security**, **policy tags**, and **ACID table semantics** over files in **GCS (and even S3/ADLS)**.
* **BigQuery**: unified engine to query **native tables + BigLake tables** together.
* **Dataplex**: discovery, taxonomy/policy tags, lifecycle, lineage across lake & warehouse.

**Why teams choose it**

* Reduce **duplication** & **data silos**.
* Serve **BI + DS/ML** from one governed copy.
* Keep **open formats** while gaining warehouse-grade controls.

**Key capabilities you’ll see called out**

* ✅ Supports most data formats (Parquet/ORC/Avro/CSV/JSON).
* ✅ **Schema-on-read** *and* **schema-on-write**.
* ✅ Unified access for **analysts, scientists, engineers**.
* ✅ **Fine-grained** governance (row/column/policy tags).
* ✅ **ACID** on managed tables; transaction safety for analytics.

---

## 5) 🗺️ Choosing guide (print-worthy)

**Pick a Data Warehouse (BigQuery) when…**

* Finance/BI needs **fast, consistent SQL** on curated, structured data.
* KPI dashboards & ad-hoc queries dominate.

**Pick a Data Lake (Cloud Storage) when…**

* You need **cheap, massive** storage for raw data; ML experimentation; future-unknown use.
* You want to **ingest quickly** with minimal upfront modeling.

**Pick a Lakehouse (BigQuery + BigLake [+ Dataplex]) when…**

* You need **BI + AI/ML** on **one governed copy**, across **files + tables** (break silos).
* You want **open formats** + **warehouse governance/perf** without copying data.

> 💡 **Exam Tips**
>
> * “**Metadata & governance over open files in object storage**” → **Lakehouse (BigLake + BigQuery)**.
> * “**Warehouse can’t handle unstructured/semi-structured easily**” → call out **DW inflexibility**.
> * “**Schema-on-read** reservoir of raw multi-type data” → **Lake**.
> * “Do it all (BI + DS/ML) on one governed copy” → **Lakehouse**.

---

## 6) 🔗 How pieces fit together on GCP (mental architecture)

```
[Producers: DBs | Apps | IoT | SaaS | Files]
          │
          ├── Land raw →  Cloud Storage  (lake)
          │                 └─ Open formats; governed by Dataplex
          │
          ├── Curate →     BigQuery      (warehouse)
          │                 └─ Modeled tables for BI
          │
          └── One layer →  BigLake over GCS/S3/ADLS
                            └─ Query files in place with BigQuery,
                               enforce row/column security, policy tags
```

---

## 7) ✅ Micro-check (what they love to ask)

* Define **lake vs warehouse vs lakehouse** in one sentence each.
* Explain **schema-on-read** vs **schema-on-write** and why each matters.
* Map **Cloud Storage/BigQuery/BigLake/Dataplex** to the right use.
* Name lake risks (**data swamp**) and how **Dataplex** mitigates them.
* State why lakehouse reduces **duplication** and **breaks silos**.

---

## 8) 🎯 Scenario quick answers

* “Correlate **reviews text** (files) with **sales** (tables) w/ one query, governed.”
  → **Lakehouse: BigQuery + BigLake (+ Dataplex)**.

* “Finance wants fast, interactive dashboards on structured facts/dims.”
  → **BigQuery** (warehouse).

* “Store **raw clickstream + images** cheaply; ML team will explore later.”
  → **Cloud Storage** (lake), query via **BigQuery external/BigLake** when needed.

---

### 👩‍🏫 Teacher’s nudge

Memorize this pairing: **BigQuery (DW)**, **Cloud Storage (Lake)**, **BigLake (Lakehouse control plane)**, **Dataplex (governance/catalog)**. If a question mentions **“metadata & governance over open files in object storage”**, your reflex answer is **lakehouse with BigLake**.
