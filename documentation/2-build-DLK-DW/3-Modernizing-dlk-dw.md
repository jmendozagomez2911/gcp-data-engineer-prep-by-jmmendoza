# 📘✨ README — Module 3: **Modernizing Data Warehouses with BigQuery & BigLake**

**Goal:** Upgrade your warehouse mindset to **serverless BigQuery** with **BigLake** on open formats (e.g., **Apache Iceberg**). Know how to tune cost/perf (partitioning & clustering), and how to query files in Cloud Storage **without copying**.

**Read me like this:** 1) BigQuery fundamentals → 2) Perf & cost (partition & cluster) → 3) BigLake + External tables → 4) Iceberg with BigQuery → 5) Lab quickstart → 6) Exam cheats & gotchas.

---

## 1) 🧠 BigQuery fundamentals (what changes from legacy DWs)

* **Fully managed & serverless**
  No servers, patches, or capacity planning. You load data & run SQL; BigQuery autoschedules work.

* **Separation of storage & compute**
  Colossus (storage) is independent from slots (compute). You **pay for storage** + **pay for compute used** (on-demand) or reserve slots (flat-rate/committed use) for predictable spend.

* **Slots & shuffle (how queries fly):**

    * **Slots** = virtual workers that process parts of your query in parallel.
    * **Shuffle** = BigQuery’s high-bandwidth re-partitioning of intermediate results across slots (on Google’s petabit network) to complete joins/aggregations fast.

* **Operational consequences**

    * Scale to TB–PBs **without** re-architecting.
    * No indexes/hints; design for **pruning** (partition/cluster), **columnar** storage, and **predicate pushdown**.

> **Exam tip**: If the prompt mentions *infra management, capacity planning, patching*, answer with **BigQuery is fully managed & serverless**; if costs/perf, mention **separation of storage/compute** and **slots**.

---

## 2) ⚡ Performance & Cost: **Partitioning** and **Clustering**

### Partitioning (scan less data)

Split a table by **DATE/TIMESTAMP** (or **INTEGER**). Queries with filters on the partition key touch only relevant partitions → faster & cheaper.

```sql
-- Time-based partition on transaction_date
CREATE TABLE dw.sales_partitioned
PARTITION BY DATE(transaction_date) AS
SELECT * FROM staging.sales_raw;
```

**Usage best practices**

* Always filter by partition column:
  `WHERE transaction_date BETWEEN '2025-08-01' AND '2025-08-07'`.
* Use **_PARTITIONTIME** / **_PARTITIONDATE** in legacy partitions if needed.

### Clustering (finer pruning within partitions)

Physically **orders** data by up to 4 columns. BigQuery can skip blocks within partitions.

```sql
CREATE TABLE dw.sales_part_clustered
PARTITION BY DATE(transaction_date)
CLUSTER BY customer_id, product_category AS
SELECT * FROM staging.sales_raw;
```

**When to cluster**

* High cardinality columns used in filters/joins (e.g., `customer_id`).
* Tables with many partitions where per-partition scans are still large.

> **Exam tip**: “Reduce scanned bytes on date-filtered queries” → **Partition** by date. “Frequent filters on customer_id within date ranges” → **Cluster** on `customer_id`.

---

## 3) 🐳 BigLake + External Tables (warehouse + lake, no copies)

**Problem:** Files live in GCS; you don’t want daily ETL.
**Answer:** **BigLake** lets BigQuery treat files as tables with **governance and security**, without moving data.

---

### ⚙️ How it works

* **External tables** → BigQuery table **pointers** to files (JSON, CSV, Parquet, ORC) in **GCS** or to **Iceberg** tables.
* **Access delegation** → Users query via BigQuery IAM; BigQuery reads GCS using a **service account**.

  * Analysts **don’t** need direct bucket access.
  * The **service account** (a non-human identity for Google services) holds `roles/storage.objectViewer` on the bucket.
  * BigQuery uses this account to **delegate access**, applying governance rules at the BigQuery layer.

---

### 🔐 Security & Governance

* Row- and column-level security, policy tags, and fine-grained access controls all apply **on** BigLake tables.
* Permissions are enforced in BigQuery IAM — even when data physically stays in **Cloud Storage**.
* Centralized governance across **warehouse + lake** → same interface, same security.

---

### 🧠 Key concept: Service account

A **service account** is a special GCP identity for code or services (not humans).
It authenticates BigQuery when it accesses external data sources.
So, when you enable **access delegation**, queries run under **your BigQuery role**, but GCS files are read using **the service account’s permissions** — keeping your users isolated from raw storage.

---

> **Exam tip:**
> “Analyst must query JSON in GCS but must not have bucket perms” →
> ✅ **BigLake external table + access delegation (service account)**.


---

## 4) 🧊 Apache Iceberg with BigQuery (open tables, warehouse features)

**Why Iceberg**: Table semantics on files—**schema evolution**, **hidden partitioning**, **time travel**, **atomic commits**.

**BigQuery + BigLake provide first-class Iceberg support**:

* Read **and** write DML: `UPDATE`, `DELETE`, `MERGE` on Iceberg tables.
* Uses Iceberg **metadata** for partition/file pruning & stats pushdown.

### Create BigLake over CSV → materialize as **Iceberg** in GCS

1. **External (staging) table over CSV in GCS**

* In Studio: *Create table* → Source **GCS** URI → **External Table** → check **Create a BigLake table using a Cloud resource connection** (or via SQL).

2. **Create Iceberg table in your GCS bucket with BigQuery**

```sql
CREATE TABLE cymbal_lake.iceberg_web_log
WITH CONNECTION `projects/PROJECT_ID/locations/REGION/connections/gcs-bucket-PROJECT_ID_eds`
OPTIONS (
  table_format = 'ICEBERG',
  storage_uri  = 'gs://gcs-bucket-PROJECT_ID'
) AS
SELECT * FROM `cymbal_lake.web_log`;   -- the external CSV table
```

> If you get “Permission denied while writing data… `gcp-sa-bigquery-condel`”, grant that **service account** `Storage Object User` on the bucket and re-run.

3. **Query Iceberg directly; join with native BQ**

```sql
-- Read from Iceberg
SELECT * FROM cymbal_lake.iceberg_web_log LIMIT 1000;

-- Join Iceberg (GCS) with native BigQuery table
SELECT wl.*, cd.*
FROM cymbal_lake.iceberg_web_log AS wl
JOIN customers.customer_details AS cd
  ON wl.customer_id = cd.id
LIMIT 1000;
```

> **Exam tip**: “Open formats + time travel + partition pruning + DML on files” → **Iceberg via BigLake**.

---

## 5) 🧪 Lab quickstart (BigLake on Iceberg)

* Enable **BigLake API**. Ensure your user has `BigQuery Connection Admin` & `BigQuery Connection User`.
* **Create Cloud Resource Connection** to GCS (Studio → Add data → Storage/Data Lakes).
* Build external table on CSV (`cloud-training/OCBL462/cymbal_synthetic_weblog_data.csv`), **check** “Create a BigLake table using a Cloud resource connection”.
* **CTAS to Iceberg** (SQL above). If permission error → give the `gcp-sa-bigquery-condel` service account **Storage Object User** on bucket, **Uniform** access.
* Query Iceberg & **JOIN** with native BigQuery table.

---

## 6) 🧠 Quick decision cheats (exam)

* **Why BigQuery over legacy DW?** Serverless, fully managed, slots + shuffle, independent storage/compute.
* **Lower cost scans?** **Partition** by date; **Cluster** by common filters/joins.
* **Query files without copying + enforce security?** **BigLake external tables** with **access delegation**.
* **Open table features over GCS files?** **Apache Iceberg**; BigQuery understands metadata and supports **DML**.
* **Governance**: Apply **row/column security** & **policy tags** on BigLake tables; end users don’t need GCS perms.

---

## 7) 🧯 Common gotchas & fixes

* **“Permission denied writing to GCS during Iceberg CTAS”**
  Grant the surfaced **`gcp-sa-bigquery-condel`** service account **Storage Object User** on the target bucket (uniform access).

* **Queries still expensive after partitioning**
  Check you **filter on the partition column**; else all partitions scan. Consider **clustering**.

* **External/BigLake feels slower**
  It’s normal—data is external. Use **predicate pushdown** (filters), consider **Iceberg** with hidden partitioning, or land hot subsets in **native BigQuery**.

---

### 👩‍🏫 Teacher’s nudge

Modernizing isn’t just “move to BigQuery.” It’s **keep raw data open in GCS**, give it **table semantics with Iceberg**, **govern** via **BigLake**, and use **partition+cluster** where it matters. If the scenario screams **no duplication, open formats, governed access**, your one-liner is:
**“BigQuery + BigLake over Iceberg in Cloud Storage, with partitioning & clustering to minimize scanned bytes.”**
