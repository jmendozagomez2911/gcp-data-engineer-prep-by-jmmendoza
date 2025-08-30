# 📦🚚 **README — Module 03: Data Replication & Migration **


**Goal:** Choose the **right tool** to bring data into GCP and understand how **CDC** and **bulk transfers** work end-to-end.
**Use this guide:** Read top-down. Skim **Exam Tips**. Copy commands/SQL to practise.

---

## 1) 🧭 Baseline Architecture (Replicate & Migrate stage)

Purpose: **bring data from external/internal systems into Google Cloud** so you can later **transform** and **store** it.

**Where data can come from**

* On-prem & multi-cloud: **file systems**, **object stores** (S3, Azure Blob), **HDFS**, **relational DBs** (Oracle, MySQL, PostgreSQL, SQL Server), **NoSQL**.

Got it 👍 You want everything blended into **one seamless narrative**, not split answers. Let me rewrite the whole thing so it flows naturally as a single explanation of **how data lands in GCP**, while also covering **Datastream, Dataflow, DMS, and transfers** together.

---

**🚀 How Data Lands in GCP**

As a data engineer in GCP, you’ll see multiple entry points for data depending on whether it’s **bulk transfers**, **scheduled jobs**, or **continuous streaming**.

---

#### 📦 One-off or Scheduled Transfers

For bulk loads or recurring file transfers, data typically lands in **Cloud Storage** (data lake) or directly in **BigQuery**.

* **Storage Transfer Service** is used to move files (e.g. AWS S3 → GCS) at scale, with scheduling options (daily, hourly, etc.).
* **BigQuery Data Transfer Service** covers SaaS apps (Google Ads, YouTube, Campaign Manager), loading them directly into **BigQuery** without staging in GCS.

💡 **Exam tip**: If you see “Files from S3 to GCS daily,” the answer is **Storage Transfer Service**.

---

#### 🔄 Continuous Change Data Capture (CDC)

When you need **ongoing sync of databases** into GCP for analytics or replication:

* **Datastream** captures inserts, updates, and deletes from **Oracle, MySQL, PostgreSQL**.
* Data can flow:

    * **Datastream → BigQuery** for real-time analytics.
    * **Datastream → GCS** (landing raw Avro/JSON) with an optional **Dataflow** step in between for **cleaning, enrichment, or transformations**, before pushing to BigQuery.

💡 **Exam tip**: “Continuous DB changes into BigQuery analytics” → **Datastream → BigQuery** (or with Dataflow if transformation needed).

---

#### ⚙️ Other Migration Helpers

For broader database moves and complex data pipelines, GCP provides extra tools:

* **Database Migration Service (DMS)** → managed service to migrate **MySQL, PostgreSQL, SQL Server, Oracle** databases into Cloud SQL, AlloyDB, or Spanner. Best for **application DB migrations**.
* **Dataflow (Apache Beam)** → a fully managed service for both **batch and streaming** data pipelines. Commonly used to:

    * Process streams from **Pub/Sub** or **Datastream**.
    * Transform files in **Cloud Storage** before loading into **BigQuery**.
    * Use **prebuilt Dataflow templates** for formats like Avro, Parquet, or integrations with NoSQL systems.


## 2) ⚙️ Toolbox Overview (when to use what)

* **`gcloud storage`** (CLI): ad-hoc **small/medium** copies to **GCS** from local/HDFS/object stores.

  ```bash
  gcloud storage cp ./path/file.csv gs://my-bucket/
  ```

Perfect 👍 let’s enrich that section with a bit more **context and nuance** so it flows naturally with the rest of the “how data lands in GCP” story. I’ll expand on **when you’d use each tool**, **extra capabilities**, and tie it to exam-style thinking.

---

#### 📦 Moving Data into GCP (Transfers & Streaming)

When bringing data into GCP, you’ve got multiple options depending on **volume, frequency, and bandwidth**.

---

#### 🔹 Quick / Manual Transfers

* For small or **ad-hoc copies** (e.g., developer moving files from a laptop).
* Tools:

    * **`gcloud storage cp`** → CLI for single uploads/downloads.
    * **gsutil** (legacy, being replaced by gcloud) → scripting/automation friendly.
* Best for **one-off moves**, **debugging**, or **small datasets**.

---

#### 🔹 Storage Transfer Service (STS)

* **Managed service** for **large-scale, online, high-speed transfers**.
* Sources: **on-prem, HDFS, AWS S3, Azure Blob, GCS buckets**.
* Features:

    * Throughput up to **tens of Gbps**.
    * **Incremental sync** (only new/changed files).
    * **Checksums & retries** for reliability.
    * **Scheduling** → automate daily/hourly sync jobs.
* Use cases:

    * “Sync my S3 bucket to GCS daily.”
    * “Move hundreds of TBs from on-prem HDFS.”

---

#### 🔹 Transfer Appliance (TA)

* **Physical hardware** shipped to your site.
* Load data onto appliance → ship back → Google uploads to **Cloud Storage**.
* Sizes: **100 TB, 480 TB, 1 PB+**.
* Best for:

    * **Petabyte-scale migrations**.
    * Sites with **limited bandwidth** or **air-gapped security constraints**.
    * Faster & safer than weeks/months of online transfer.

---

#### 🔹 Datastream (CDC → GCP)

* **Serverless Change Data Capture** service.
* Sources: **Oracle, MySQL, PostgreSQL, SQL Server, AlloyDB**.
* Destinations: **GCS (raw)** or **BigQuery (analytics)**.
* Features:

    * **Backfill** historical data + **ongoing change capture**.
    * Granular: choose schema, tables, or columns.
    * Secure connectivity (VPC peering, private connectivity).
* Often combined with **Dataflow templates** to:

    * Transform Avro/JSON → Parquet.
    * Clean/enrich data before BigQuery.

---

#### 🔹 Destination Choices

* **Cloud SQL / AlloyDB** → transactional (OLTP) workloads.
* **BigQuery** → analytical (OLAP) workloads.
* **Cloud Storage** → raw landing zone (data lake).

---

#### 📊 Bandwidth Considerations (intuition)

* **1 TB @ 100 Gbps ≈ 2 minutes**
* **1 TB @ 100 Mbps ≈ 30 hours**
  👉 If your link is **fast** → STS works well.
  👉 If your link is **slow** and data is **huge** → TA is more efficient.

---

### 💡 Exam Tips

* **“Petabytes + low bandwidth” → Transfer Appliance.**
* **“Ongoing daily sync from S3” → Storage Transfer Service with schedule.**
* **“Continuous DB changes into BigQuery” → Datastream (with optional Dataflow).**
* **“One-time small copy from laptop” → gcloud storage cp.**

---

## 3) 🌊 Datastream Deep Dive (CDC)

**What it does**

* Listens to source DB **logs** to capture **INSERT/UPDATE/DELETE** in near real time.
* Sources & their logs:

  * **Oracle** → **LogMiner**
  * **MySQL** → **Binary Log**
  * **PostgreSQL** → **Logical Decoding / WAL**
  * **SQL Server** → **Transaction Log**
* Outputs **events** to **GCS** (e.g., **Avro/JSON**) or directly into **BigQuery** tables.
* Can route through **Dataflow** for transformation or event-driven architectures.

**Event structure**

* **Metadata** (generic): source table, timestamps, operation, etc.
* **Payload**: key-value pairs of **column → value**.
* **Source-specific metadata**: database/schema/table, change type (e.g., INSERT), source IDs.

Here’s a clearer, more precise version of that section 👇

---

#### 🔹 Unified Data Types

Datastream standardises database-specific numeric types so that downstream systems can handle them consistently:

* **Source normalisation**: Different DBMS types (e.g., Oracle `NUMBER`, MySQL `DECIMAL`, PostgreSQL `NUMERIC`, SQL Server `DECIMAL`) are **normalised to a generic decimal type** during replication.

* **Landing formats**:

    * **Avro (Cloud Storage)** → stored as **decimal** (preserves precision and scale).
    * **JSON (Cloud Storage)** → stored as **number** (JSON only supports a generic numeric type, no explicit precision).
    * **BigQuery** → mapped to native **NUMERIC** (supports up to 38 digits precision and 9 digits scale).

➡️ This approach ensures **cross-database consistency**, so data from heterogeneous sources aligns to a predictable type system without losing precision.

---

**Deployment patterns**

* **Direct to BigQuery** for analytics.
* **Via GCS → Dataflow → BigQuery** for custom transforms or event fan-out.

> 💡 **Exam Tip**
> Keywords “**near real-time**”, “**CDC**”, “**replicate Oracle/MySQL/Postgres/SQL Server**”, “**select specific schemas/tables/columns**” → **Datastream**.

---

## 4) 🧪 Hands-On Lab (PostgreSQL → BigQuery with Datastream)

### A. Prepare Cloud SQL for PostgreSQL

Enable API:

```bash
gcloud services enable sqladmin.googleapis.com
```

Create instance (example flags from lab):

```bash
POSTGRES_INSTANCE=postgres-db
DATASTREAM_IPS=IP_ADDRESS   # region-specific Datastream public IPs
gcloud sql instances create ${POSTGRES_INSTANCE} \
  --database-version=POSTGRES_14 \
  --cpu=2 --memory=10GB \
  --authorized-networks=${DATASTREAM_IPS} \
  --region=REGION \
  --root-password pwd \
  --database-flags=cloudsql.logical_decoding=on
```

Connect & create schema/data:

```bash
gcloud sql connect postgres-db --user=postgres  # password: pwd
```

```sql
CREATE SCHEMA IF NOT EXISTS test;
CREATE TABLE IF NOT EXISTS test.example_table (
  id SERIAL PRIMARY KEY,
  text_col VARCHAR(50),
  int_col INT,
  date_col TIMESTAMP
);
ALTER TABLE test.example_table REPLICA IDENTITY DEFAULT;

INSERT INTO test.example_table (text_col, int_col, date_col) VALUES
('hello',0,'2020-01-01 00:00:00'),
('goodbye',1,NULL),
('name',-987,NOW()),
('other',2786,'2021-01-01 00:00:00');
```

Enable replication artifacts:

```sql
CREATE PUBLICATION test_publication FOR ALL TABLES;
ALTER USER POSTGRES WITH REPLICATION;
SELECT PG_CREATE_LOGICAL_REPLICATION_SLOT('test_replication', 'pgoutput');
```

### B. Create Datastream resources

1. **Connection profiles**

   * **Source (PostgreSQL)**: `postgres-cp` → REGION, public IP of `postgres-db`, user `postgres`/pwd `pwd`, DB `postgres`; allowlist IP; **RUN TEST**.
   * **Destination (BigQuery)**: `bigquery-cp` → REGION.

2. **Stream**

   * Name: `test-stream`, REGION.
   * Source type: **PostgreSQL** (profile: `postgres-cp`).
   * Replication slot: `test_replication`; Publication: `test_publication`.
   * Select **schema `test`**.
   * Destination: **BigQuery** (profile: `bigquery-cp`), dataset location **REGION**, **staleness limit = 0s**.
   * **Run Validation** → **Create & Start**. Wait until **Running**.

### C. Validate in BigQuery

Open **BigQuery Studio** → expand dataset `test` → table `example_table` → **PREVIEW**.
If not visible yet, run:

```sql
SELECT * FROM test.example_table ORDER BY id;
```

### D. Prove CDC (changes flow through)

Connect back to Cloud SQL:

```bash
gcloud sql connect postgres-db --user=postgres   # pwd
```

Apply changes:

```sql
INSERT INTO test.example_table (text_col, int_col, date_col) VALUES
('abc',0,'2022-10-01 00:00:00'),
('def',1,NULL),
('ghi',-987,NOW());

UPDATE test.example_table SET int_col = int_col * 2;
DELETE FROM test.example_table WHERE text_col='abc';
```

Query in BigQuery:

```sql
SELECT * FROM test.example_table ORDER BY id;
```

---

## 5) 🧠 Choosing the Right Option (decision cheats)

**Dataset size & network**

* **Small/medium, ad-hoc** → `gcloud storage cp`
* **Large, scheduled, multi-cloud** → **Storage Transfer Service**
* **Massive or low bandwidth** → **Transfer Appliance**
* **Continuous DB changes** → **Datastream** (CDC)
* **Full DB migration for apps** → **Database Migration Service**

**Landing target**

* Analytics (OLAP) → **BigQuery**
* App DB (OLTP) → **Cloud SQL** / **AlloyDB**
* Pre-process files → **GCS** (then Dataflow/Dataproc)

**Processing**

* Simple load/ELT → **BigQuery**
* Stream transforms/windows/exactly-once → **Dataflow**
* Existing Spark jobs → **Dataproc**

---

## 6) ✅ Micro-Checklist for the Exam

* Difference between **`gcloud storage`**, **STS**, **Transfer Appliance**, **Datastream**, **DMS**.
* Bandwidth sizing intuition (1 TB: **100 Gbps ≈ \~2 min**, **100 Mbps ≈ \~30 hr**).
* **Datastream**: sources, **log types** (LogMiner/binlog/WAL/txn log), **backfill + ongoing**, **selective replication**, **Avro/JSON**, **direct to BQ** or via **Dataflow**.
* When to land in **GCS vs BigQuery**, and how **CDC** supports near real-time analytics.
* Lab flow: prepare **Cloud SQL**, create **connection profiles**, **stream**, validate in **BigQuery**, then **mutate source** and re-check.

---

### 👩‍🏫 Final Thought

Replication is about **fit-for-purpose movement**: pick the **right lane** (CLI, STS, TA, Datastream), **land** in the right place (**GCS/BQ**), and **prove** end-to-end with queries. If you can **map the scenario to the tool** and **explain why**, you’re set for the exam.
