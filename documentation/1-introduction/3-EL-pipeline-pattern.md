# 📘✨ **README — Module 03: The Extract & Load (EL) Data Pipeline Pattern**

**Goal:** Know *how to get data into BigQuery fast* without upfront transforms, and when to use **bq**, **BigQuery Data Transfer Service**, **External Tables**, and **BigLake**.
**How to use:** Read top-down. Skim **Exam Tips**. Copy the commands & SQL to practise.

---

## 1) 🧭 What is “Extract & Load” (EL)?

**EL = Extract data from sources and Load it to BigQuery** (no transform step first).
Why it matters: **simpler ingestion**, easier automation & scheduling, and **faster time-to-analysis**.

**How EL lands data in BigQuery**

* Direct **loads** via **UI**, **SQL (`LOAD DATA`)**, or **CLI (`bq load`)**.
* **Managed transfers** via **BigQuery Data Transfer Service** (BDTS).
* **Zero-copy access** via **External Tables** or **BigLake** --> query data *in place* in (Cloud Storage / other object stores) without loading.

> 💡 **Exam Tip**
> If the scenario says “*no transformations yet*”, “*load first*”, or “*query files in GCS directly*”, think **EL** with **bq load / BDTS / External / BigLake**.

---

## 2) 🏗️ Baseline EL Architecture

1. **Sources** → files (Avro/Parquet/ORC/CSV/JSON), SaaS, other warehouses, Google Sheets, Bigtable, etc.
2. **Load or Reference** →

    * **Load** with **bq / UI / SQL** or **BDTS**, **OR**
    * **Reference** with **External Tables** / **BigLake** (no copy).
3. **Analyze** → BigQuery SQL (optionally transform later = **ELT**).

---

## 3) 📥 BigQuery: formats you can **load & export**

* **Load**: **Avro, Parquet, ORC, CSV, JSON**, and **Firestore exports**.
* **Export** (tables/query results): **CSV, JSON, Avro, Parquet**.

> 💡 **Exam Tip**
> Prefer **Parquet/ORC/Avro** for columnar & typed loads → faster & cheaper than CSV.

---

## 4) 🖱️ UI & 🧾 SQL: two built-in ways to load

* **UI (Console)**: point-and-click upload, pick format, **auto-detect schema**.
* **SQL**: `LOAD DATA` (good for automation; supports append/overwrite).

Example (SQL):

```sql
LOAD DATA INTO dataset.table
FROM FILES (
  format = 'CSV',
  uris = ['gs://bucket/path/file*.csv'],
  skip_leading_rows = 1
);
```

---

## 5) 💻 **bq** Command-Line (Cloud SDK)

Programmatic control of BigQuery.

* Create datasets/tables: `bq mk`
* **Load data**: `bq load` (supports wildcards & schema files)

Common pattern:

```bash
# With explicit schema file (schema.json) and header skip
bq load \
  --source_format=CSV \
  --skip_leading_rows=1 \
  dataset.table \
  gs://my-bucket/path/*.csv \
  schema.json
```

Key flags to remember:

* `--source_format` (CSV/NEWLINE\_DELIMITED\_JSON/AVRO/PARQUET/ORC)
* `--skip_leading_rows`
* Append vs overwrite: `--noreplace` (append) or `--replace` (truncate+load)

> 💡 **Exam Tip**
> “Multiple files in GCS” → **wildcards** in `bq load`. Need structure enforced → provide **schema file**.

---

## 6) 🔁 **BigQuery Data Transfer Service (BDTS)**

**Managed, serverless, no-code** transfers into BigQuery.

* Sources: **SaaS apps, object stores (S3/Azure Blob), other warehouses**, GCS, etc.
* **Scheduling**: recurring or on-demand; configure source params & destination settings.
* No infra to manage.

> 💡 **Exam Tip**
> “Recurring daily import from S3 into BigQuery” → **BDTS**.

---

Here’s your enriched section with the **“query in place” vs “query in object stores” nuance** integrated seamlessly so it flows without breaking your structure:

---

## 7) 🔍 **External Tables** vs 🐳 **BigLake** (non-copy EL)

### External Tables (BigQuery)

* Query data **in place** from **GCS**, **Google Sheets**, **Bigtable**, etc. (broader scope: not just object stores).
* Pros: zero copy, quick setup, good for **ad-hoc exploration** or lightweight pipelines.
* **Limitations**: may be **slower** (scans raw files), no **table preview**, no **query caching**, and **no cost estimation** (can’t predict scan cost before running).
* **Permissions model**: user needs **both BigQuery table access** *and* **direct access** to the underlying source (e.g., bucket). This can complicate governance at scale.
* Common exam scenario: “Query CSV in GCS directly without loading” → **External Table**.

---

### BigLake (unified lakehouse access)

* Query data **in object stores** (**GCS, AWS S3, Azure Data Lake Storage Gen2**) as if it were a **native BigQuery table** (joins, SQL, federated queries).
* Difference in scope: **all BigLake queries are “in place”**, but specifically targeted at **object stores** (lakehouse pattern).
* **Performance optimisations**:

    * Uses **Apache Arrow** for columnar access.
    * Maintains a **metadata cache** (row counts, min/max values, file stats) → enables **predicate pushdown** and **file pruning**.
    * **Cache staleness** tunable: **30 min → 7 days**; refresh can be **auto or manual**.
    * Spark + Presto can also use these cached stats via the **BigLake connectors**.
* **Security & Governance**:

    * Data access via **service account delegation**, so end-users don’t need bucket-level access.
    * Supports **row-level** and **column-level security** + policy tags, integrating with **Dataplex** for central governance.
* **Trade-offs**:

    * Like External, still **no preview** and **no cost estimation**.
    * Slightly slower than native BigQuery tables (since data isn’t persisted in BQ storage), but faster than plain External tables due to caching/pruning.

---

### ✅ When to choose

* **External Tables** → lightweight use cases, **fast prototyping**, or when governance is less strict; works for **varied external sources** (GCS, Sheets, Bigtable).
* **BigLake Tables** → **enterprise-grade**, secure, multi-cloud **object store access** with fine-grained security and performance improvements.

---

>💡 **Exam Tip**
>* “Query Parquet in GCS, no transformations, one-off analysis” → **External Table**.
>* “Need **row/column security**, multi-cloud access, or strict governance” → **BigLake**.

---

## 8) 🧪 Lab Recap — **BigLake: Qwik Start**

### Task 1 — Create a **Connection Resource**

* BigQuery Studio → **+ADD → Connections to external data sources**
* Type: **Vertex AI remote models, remote functions and BigLake (Cloud Resource)**
* **Connection ID:** `my-connection`
* **Location:** **US (multi-region)**
* Copy the **Service Account ID**.

### Task 2 — Grant GCS Access to the Connection

* **IAM & Admin → IAM → +GRANT ACCESS**
* Principal: **(connection’s service account)**
* Role: **Storage Object Viewer**

> After moving to BigLake, **remove direct GCS permissions** from end users to enforce governance via BigQuery.

### Task 3 — Create a **BigLake** Table

1. Create dataset **`demo_dataset`** (US, multi-region).
2. **Create table** → **Source: Google Cloud Storage** → select **`customer.csv`** in your project bucket.
3. **Destination**: `demo_dataset.biglake_table`
4. **Table type:** **External Table**
5. ✔️ **Create a BigLake table using a Cloud Resource connection** → select **`us.my-connection`**
6. **Schema** (paste provided JSON with `customer_id`, `first_name`, … `support_rep_id`).
7. **Create Table**.

### Task 4 — Query via BigQuery

```sql
SELECT * FROM `PROJECT_ID.demo_dataset.biglake_table`;
```

### Task 5 — Column-Level Security (Policy Tags)

* A taxonomy **`taxonomy name`** and tag **`biglake-policy`** exist.
* In schema editor, **tag** columns: `address`, `postal_code`, `phone`.
* Query with all columns → **Access Denied**.
* Works when excluding protected columns:

```sql
SELECT * EXCEPT(address, phone, postal_code)
FROM `PROJECT_ID.demo_dataset.biglake_table`;
```

### Task 6 — **Upgrade an External Table** to BigLake

Create a plain **external** table `demo_dataset.external_table` on **`invoice.csv`** (no connection yet; use the provided schema with `invoice_id`, `customer_id`, `invoice_date`, … `total`).

Now upgrade it:

```bash
export PROJECT_ID=$(gcloud config get-value project)

# Create external table definition that *uses* the connection
bq mkdef \
  --autodetect \
  --connection_id=$PROJECT_ID.US.my-connection \
  --source_format=CSV \
  "gs://$PROJECT_ID/invoice.csv" > /tmp/tabledef.json

# (Optional) Inspect def
cat /tmp/tabledef.json

# Grab existing schema
bq show --schema --format=prettyjson demo_dataset.external_table > /tmp/schema

# Update the table to BigLake by attaching the connection + schema
bq update --external_table_definition=/tmp/tabledef.json \
          --schema=/tmp/schema \
          demo_dataset.external_table
```

Verify in **BigQuery Studio → Details** that **External Data Configuration** shows the **Connection ID**.

---

## 9) 🧠 Quick Decision Cheats

* **Need recurring pulls from SaaS / S3 / other warehouses into BQ?** → **BDTS**
* **One-off/automated CLI loads from GCS?** → **`bq load`** (or `LOAD DATA` SQL)
* **Query files without copying?** → **External Table**
* **Zero-copy + fine-grained security + multi-cloud lake access?** → **BigLake**
* **Fast schema-aware loads?** → Use **Parquet/ORC/Avro** over CSV

---

## 10) ✅ Micro-Checklist for the Exam

* Know **EL** vs **ELT**: here we **load first** (no upfront transform).
* **bq load** flags: `--source_format`, `--skip_leading_rows`, wildcards, schema file; append vs overwrite.
* **BDTS**: managed, scheduled, no-code transfers from SaaS/object stores/warehouses.
* **External vs BigLake**: performance limits, preview/cost estimation behavior, **permissions model** (direct source perms vs **delegation via service account**), **row/column security**.
* **BigLake internals**: **Apache Arrow**, **metadata cache** (min/max, row count), **staleness 30 min–7 days**, pushdown & pruning, Spark connector leverage.

---

## 11) 🎯 Practice Prompts

1. “Query Parquet in GCS weekly without copying; we need column-level security.” → **BigLake**.
2. “Nightly ingest from S3 to BigQuery, no code preferred.” → **BDTS**.
3. “Load 200 CSV files from one GCS prefix and skip headers.” → `bq load --source_format=CSV --skip_leading_rows=1 dataset.table gs://bucket/prefix/*.csv schema.json`
4. “Analyst must query a Google Sheet in BigQuery.” → **External table** on **Sheets**.
5. “Why is query slower and no preview on this table?” → It’s **External/BigLake** (data is external; some features disabled).

---

### 👩‍🏫 Teacher’s nudge

Master the **menu of EL options** (UI/SQL/bq/BDTS/External/BigLake) and when to use each. If you can **map a scenario to the right tool** and explain **why**, you’ll nail this module on the exam.
