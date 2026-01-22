# 📘✨ **README — Module 03: The Extract & Load (EL) Data Pipeline Pattern**

**Goal:** Get data into **BigQuery fast** without upfront transforms, and choose correctly between **UI**, **`LOAD DATA`**, **`bq`**, **BigQuery Data Transfer Service**, **External tables**, and **BigLake tables**.

**Read me like this:** 1) what EL is → 2) load methods (UI/SQL) → 3) `bq` CLI → 4) Data Transfer Service → 5) query-in-place (External vs BigLake) → 6) BigLake lab + upgrade external → 7) exam cheats + quiz mapping.

---

## 1) 🧭 What is “Extract & Load” (EL)?

**EL = Extract data + Load it into BigQuery** (no transformation step first).

Why it matters (module framing):

* Eliminates upfront transformation → **simplifies ingestion**
* Enables **scheduling** (managed transfers / automated loads)
* Supports **no-copy / no-move** access to data using **External tables** and **BigLake tables**

> 💡 **Exam Tip**
> If the scenario says “load first”, “no transforms yet”, “simplify ingestion”, or “query data in GCS without copying” → think **EL**.

---

## 2) 🏗️ Baseline EL architecture (mental model)

1. **Sources** → files and systems producing data
2. **Make data available in BigQuery** using one of two approaches:

   * **Load** into BigQuery storage (best performance)
   * **Query in place** (data stays in external source)
3. **Analyse** using BigQuery SQL (transform later if needed → ELT)

---

## 3) 📥 BigQuery formats you can load/export (must know)

**Load supports:**

* **Avro, Parquet, ORC, CSV, JSON**, plus **Firestore exports**

**Export supports (tables/query results):**

* **CSV, JSON, Avro, Parquet**

> 💡 **Exam Tip**
> Format lists are often tested as elimination questions. If it’s about *loading/exporting*, these are your safe answers.

---

## 4) 🖱️ UI vs 🧾 SQL (`LOAD DATA`) — the two built-in load methods

### A) UI (Console)

* Friendly upload flow
* Choose file + format
* Option to **auto-detect schema**

### B) `LOAD DATA` SQL

* More control (automation-friendly)
* Supports **append** and **overwrite** patterns (table load behaviour)

Example shape:

```sql
LOAD DATA INTO dataset.table
FROM FILES (
  format = 'CSV',
  uris = ['gs://bucket/path/file*.csv'],
  skip_leading_rows = 1
);
```

> 💡 **Exam Tip**
> “Automate a load inside a script/app and control append/overwrite” → **`LOAD DATA`**.

---

## 5) 💻 `bq` CLI (Cloud SDK) — programmatic BigQuery control

`bq` is your CLI for creating objects and loading data.

### A) Create datasets/tables

```bash
bq mk --dataset my_dataset
```

### B) Load data (`bq load`)

Key parameters the module calls out:

* source format (CSV etc.)
* skip header rows
* target dataset.table
* wildcards for multiple GCS files
* optional schema file

Common pattern:

```bash
bq load \
  --source_format=CSV \
  --skip_leading_rows=1 \
  dataset.table \
  gs://my-bucket/path/*.csv \
  schema.json
```

Remember:

* Append vs overwrite: `--noreplace` (append) vs `--replace` (overwrite)

> 💡 **Exam Tip**
> “Load many files from GCS” → use **wildcards** in `bq load`.
> “Need structure enforced” → provide a **schema file**.

---

## 6) 🔁 BigQuery Data Transfer Service (BDTS)

**BDTS = fully managed, serverless, no-code** scheduled transfers **into BigQuery**.

What it’s for (module wording):

* Load structured data from:

   * **SaaS applications**
   * **object stores**
   * **other data warehouses**
* Scheduling:

   * **recurring** or **on-demand**
* Config-driven: source details + destination settings
* No infra / no pipelines to operate

> 💡 **Exam Tip**
> “Recurring scheduled transfer into BigQuery, no-code, managed” → **BDTS**.

---

## 7) 🔍 Query in place: **External tables** vs 🐳 **BigLake tables**

This is the high-yield part of the module: **same idea (query external data)**, but very different governance + performance model.

### A) External tables (BigQuery)

BigQuery can query data **outside** its native storage, e.g.:

* **Cloud Storage**
* **Google Sheets**
* **Bigtable**

Use case from transcript:

* Query data in Cloud Storage **without loading it into BigQuery**, especially for **less frequent access**

**Known limitations (explicitly called out):**

* Can be **slower**
* **No cost estimation**
* **No table preview**
* **No query caching**

**Permissions model (important):**

* Users often need **separate permissions**:

   * access to the BigQuery external table **and**
   * access to the underlying source (e.g. GCS bucket)

> 💡 **Exam Tip**
> “Query GCS/Sheets/Bigtable directly via BigQuery” → **External table** (but expect limitations).

---

### B) BigLake tables

**BigLake extends BigQuery** to query data in your **data lake / object stores** with:

* **better performance** (vs plain external tables) using caching/Arrow
* **fine-grained security** (row-level + column-level)
* **multi-cloud object store reach** (GCS + other cloud object stores)

The module’s key claims:

**Performance**

* Uses **Apache Arrow**
* Maintains a **metadata cache** to accelerate queries:

   * file size
   * row count
   * min/max column stats
* Enables:

   * faster pruning (files/partitions)
   * dynamic predicate pushdown
   * avoids listing all objects each time

Cache settings:

* **Staleness configurable: 30 minutes → 7 days**
* Refresh: **automatic or manual**
* Spark can leverage the cache stats via **Spark–BigQuery connector**

**Security/Governance**

* Access is **delegated via a service account**
* Decouples **table access** from **storage access**
* Makes access management simpler and more secure than external tables
* Supports **row-level and column-level security** (the lab demonstrates column policy tags)

**Still true (external nature):**

* **No cost estimation**
* **No table preview**

> 💡 **Exam Tip**
> If you see “need row/column security on data in GCS (or multi-cloud object store) without loading” → **BigLake**.

---

## 8) 🧪 Lab recap — BigLake: Qwik Start (what you must be able to explain)

### Task 1 — Create a Connection Resource

* BigQuery Studio → Add data → create **Cloud Resource connection**
* Copy the **Service Account ID** (this is the delegation identity)

### Task 2 — Grant the connection SA access to GCS

* IAM: grant connection SA **Storage Object Viewer**

**Critical governance rule:**

* After migrating to BigLake, remove end users’ direct GCS access
  (otherwise they can bypass BigQuery-enforced row/column policies)

### Task 3 — Create a BigLake table (external + connection)

* Create dataset `demo_dataset` (US multi-region in lab)
* Create table from GCS `customer.csv`
* Table type: **External Table**
* Tick: “Create a BigLake table using a Cloud Resource connection”
* Provide schema (lab uses schema explicitly to show policy tags clearly)

### Task 5 — Column-level access control (policy tags)

* Apply policy tag to sensitive columns (address/postal_code/phone)
* Query `SELECT *` → **Access denied**
* Query excluding restricted columns → works:

```sql
SELECT * EXCEPT(address, phone, postal_code)
FROM `PROJECT_ID.demo_dataset.biglake_table`;
```

### Task 6 — Upgrade an External Table → BigLake Table

Key pattern:

* Use `bq mkdef` with `--connection_id`
* `bq update --external_table_definition=...`

---

## 9) 🧠 Quick decision cheats (memorise)

| Requirement                                                 | Best choice         |
| ----------------------------------------------------------- | ------------------- |
| Upload/load through console                                 | **UI load**         |
| Automate loads via SQL                                      | **`LOAD DATA`**     |
| Programmatic loads + wildcards                              | **`bq load`**       |
| Managed scheduled transfers from many sources               | **BDTS**            |
| Query GCS/Sheets/Bigtable without loading                   | **External tables** |
| Query object-store lake data with better security + caching | **BigLake tables**  |

---

## 10) ✅ Micro-Checklist for the Exam

* EL means **load without transform first**
* BigQuery load formats: **Avro/Parquet/ORC/CSV/JSON + Firestore exports**
* Load methods:

   * **UI**
   * **`LOAD DATA`** (automation + append/overwrite control)
   * **`bq load`** (format, skip headers, wildcards, schema file)
* **BDTS** = managed, serverless, scheduled transfers into BigQuery
* External vs BigLake:

   * External: simpler, more limitations, requires **separate source permissions**
   * BigLake: **Apache Arrow + metadata cache**, **delegated access via service account**, **row/column security**, **multi-cloud object stores**
   * BigLake cache staleness: **30 min to 7 days**, refresh **auto/manual**
   * Both: **no preview + no cost estimation** (because data is external)

---

## 11) 📝 Quiz mapping (what they’re testing)

1. Main advantage BigLake over external tables → **enhanced performance, security, flexibility**
2. Query Cloud Storage without loading → **External tables**
3. BigLake metadata cache staleness → **30 minutes to 7 days; refresh auto or manual**
4. `LOAD DATA` best for → **automating loading in scripts/apps**
5. BDTS is → **fully managed service for scheduling/automating transfers into BigQuery**

---

If you paste the next module draft + transcripts, I’ll apply the same treatment: remove unsupported claims, sharpen decision rules, and build the quiz logic directly into the README.
