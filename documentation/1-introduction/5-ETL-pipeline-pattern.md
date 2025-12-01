# 🛠️⚡ **README — Module 03: The Extract, Transform, and Load (ETL) Data Pipeline Pattern**

+ **Goal:** Know when to use **ETL** (transform **before** load), which Google Cloud tools fit (GUI + open-source), and how batch & streaming ETL land in **BigQuery**/**Bigtable**.
+ **Read me like this:** 1) mental model & architecture → 2) GUI tools → 3) Dataproc (batch) → 4) Dataproc Serverless for Spark → 5) Streaming (Pub/Sub + Dataflow) → 6) Bigtable’s role → 7–8) lab recaps → 9) exam cheats.

---

## 1) 🧭 What is ETL? (baseline architecture)

**ETL = Extract → Transform → Load.**
You **clean/shape/enrich** data **before** loading to an analytics store.

**Baseline flow**

1. **Extract** from sources (DBs, files, SaaS, streams).
2. **Transform** on an engine (**GUI tools, Spark, Beam, Python**).
3. **Load** into **BigQuery** (analytics) or **Bigtable** (operational/serving).

**Why choose ETL (vs ELT)**

* Regulatory/PII rules require pre-load masking/aggregation.
* Heavy reshaping best handled on **Spark/Beam**.
* Reusable pipelines that deliver **ready-to-use** curated data.

> 💡 **Exam Tip**
> If the scenario stresses **“must anonymize/validate before warehouse”** or **non-SQL heavy transformations**, answer **ETL** with **Dataproc/Dataflow** (possibly fronted by **Dataprep/Data Fusion**).

---

## 2) 🧑‍🎨 GUI tools for ETL (no/low-code)

### 🧼 Dataprep by Trifacta

* **Serverless, no-code wrangling**; visual “recipes” of transforms.
* Smart suggestions (find/replace, extract, split, dedupe).
* Executes on **Dataflow**, includes **scheduling & monitoring**.

### 🔗 Data Fusion

* **GUI data integration** (drag-and-drop pipelines).
* Connects on-prem & cloud (extensible with **plugins**).
* Executes on **Hadoop/Spark**; preview data at each stage.
* Example: **join** two SAP tables → write one branch to **GCS**, another to **BigQuery** with a transform.

> 💡 **Exam Tip**
> “Enterprise GUI integration, hybrid sources, custom plugins” → **Data Fusion**.
> “Analyst-friendly data cleaning, visual suggestions, runs on Dataflow” → **Dataprep**.

---

## 3) 🐘 Dataproc (managed Hadoop/Spark) — batch ETL

* Managed **Hadoop/Spark** on **GCE/GKE** with **workflow templates**, **autoscaling**, **ephemeral or long-lived clusters**.
* Storage options:

    * **Cloud Storage** for durable data (common; no need for HDFS on disks).
    * **HDFS on PD** if needed; connectors for **BigQuery**/**Bigtable**.
* **Workflow Templates (YAML)** define multi-step jobs with dependencies; submit via `gcloud`.
* **Spark** ecosystem: Spark SQL, Streaming, MLlib, GraphX. Languages: **SQL, Py/Scala/Java, R**.

> 💡 **Exam Tip**
> “Lift & shift Spark/Hadoop,” “custom Spark code,” “controlled cluster sizing” → **Dataproc**.

---

## 4) 🚀 Dataproc **Serverless for Spark**

* **No cluster management**; **auto-scales**, **pay-per-use**, fast startup, no resource contention.
* Two modes:

    * **Batches** (submit with `gcloud`, great for scheduled jobs).
    * **Interactive sessions** (JupyterLab; notebooks local or in GCP).
* Deep integrations: **Dataproc History Server**, **Dataproc Metastore**, **BigQuery external procedures**, **Vertex AI Workbench**, **Cloud Storage**.
* Under the hood, it spins **ephemeral clusters** per job/session.

> 💡 **Exam Tip**
> “Run Spark but don’t manage clusters” → **Dataproc Serverless (batches or notebooks)**.

---

## 5) 🌊 Streaming ETL on Google Cloud

### Batch vs Streaming (quick contrast)

* **Batch**: fixed, periodic sets (e.g., payroll, billing).
* **Streaming**: continuous events (fraud, IoT, clickstream, ops).

### Core pattern

1. **Ingest** events → **Pub/Sub** (decoupled, durable, high throughput).
2. **Process** in near real time → **Dataflow** (**Apache Beam**) with windows, state, exactly-once sinks.
3. **Load** to **BigQuery** (analytics) and/or **Bigtable** (serving/low-latency).

### Dataflow (Beam) essentials

* One codebase for **batch & streaming** (Java/Python/Go).
* **Templates** (Google-provided or custom) decouple design from deployment; parameterize inputs/outputs.
* Notebooks for rapid prototyping.

> 💡 **Exam Tip**
> Keywords **“windowing, event time, Pub/Sub, near real-time to BigQuery”** → **Dataflow**.
> Need **reusable pipelines** → **Dataflow templates**.

---

## 6) 🧱 Bigtable’s role in pipelines

* **Wide-column NoSQL** with **sub-10 ms** reads/writes, petabyte scale.
* **Row key** design = your index (optimize for access pattern).
* Great for **time series, IoT telemetry, financial ticks, personalization features**.
* Often paired with streaming:

    * **Dataflow** → **Bigtable** for serving features/operational lookups,
    * and/or → **BigQuery** for analytics.

> 💡 **Exam Tip**
> “Millisecond latency over high-throughput time-series/IoT” → **Bigtable** (not BigQuery).

---

## 7) 🧪 Lab Recap #1 — **Dataproc Serverless for Spark → BigQuery (batch)**

### Setup snippets

```bash
# Enable Private IP Google Access for default subnet
gcloud compute networks subnets update default --region=REGION --enable-private-ip-google-access

# Buckets
gsutil mb -p PROJECT_ID gs://PROJECT_ID
gsutil mb -p PROJECT_ID gs://PROJECT_ID-bqtemp

# BigQuery dataset
bq mk -d loadavro
```

### Get assets + run template (on a provided VM)

```bash
wget https://storage.googleapis.com/cloud-training/dataengineering/lab_assets/idegc/campaigns.avro
gcloud storage cp campaigns.avro gs://PROJECT_ID

wget https://storage.googleapis.com/cloud-training/dataengineering/lab_assets/idegc/dataproc-templates.zip
unzip dataproc-templates.zip
cd dataproc-templates/python

export GCP_PROJECT=PROJECT_ID
export REGION=REGION
export GCS_STAGING_LOCATION=gs://PROJECT_ID
export JARS=gs://cloud-training/dataengineering/lab_assets/idegc/spark-bigquery_2.12-20221021-2134.jar

./bin/start.sh -- --template=GCSTOBIGQUERY \
  --gcs.bigquery.input.format="avro" \
  --gcs.bigquery.input.location="gs://PROJECT_ID" \
  --gcs.bigquery.input.inferschema="true" \
  --gcs.bigquery.output.dataset="loadavro" \
  --gcs.bigquery.output.table="campaigns" \
  --gcs.bigquery.output.mode=overwrite \
  --gcs.bigquery.temp.bucket.name="PROJECT_ID-bqtemp"
```

### Validate in BigQuery

```bash
bq query --use_legacy_sql=false 'SELECT * FROM `loadavro.campaigns`;'
```

---

## 8) 🧪 Lab Recap #2 — **Streaming dashboard with Dataflow → BigQuery → Looker Studio**

### Create dataset & table (partitioned)

```bash
bq --location=Region mk taxirides
bq --location=Region mk \
  --time_partitioning_field timestamp \
  --schema ride_id:string,point_idx:integer,latitude:float,longitude:float,\
timestamp:timestamp,meter_reading:float,meter_increment:float,ride_status:string,\
passenger_count:integer -t taxirides.realtime
```

### Stage artifacts to GCS

```bash
gcloud storage cp gs://cloud-training/bdml/taxisrcdata/schema.json  gs://Project_ID-bucket/tmp/schema.json
gcloud storage cp gs://cloud-training/bdml/taxisrcdata/transform.js gs://Project_ID-bucket/tmp/transform.js
gcloud storage cp gs://cloud-training/bdml/taxisrcdata/rt_taxidata.csv gs://Project_ID-bucket/tmp/rt_taxidata.csv
```

### Dataflow job (template)

* Template: **Cloud Storage Text to BigQuery (Stream)**
* Key params:

    * Input: `gs://Project_ID-bucket/tmp/rt_taxidata.csv`
    * Schema JSON: `gs://Project_ID-bucket/tmp/schema.json`
    * Output table: `Project_ID:taxirides.realtime`
    * Temp dir: `gs://Project_ID-bucket/tmp`
    * JS UDF path: `gs://Project_ID-bucket/tmp/transform.js`
    * JS UDF name: `transform`
    * Workers: 1–2 (e2-medium)

### Query stream & aggregate

```sql
SELECT * FROM taxirides.realtime LIMIT 10;

WITH streaming_data AS (
  SELECT
    timestamp,
    TIMESTAMP_TRUNC(timestamp, MINUTE, 'UTC') AS minute,
    ride_id, latitude, longitude, meter_reading, ride_status, passenger_count
  FROM taxirides.realtime
  ORDER BY timestamp DESC
  LIMIT 1000
)
SELECT
  ROW_NUMBER() OVER() AS dashboard_sort,
  minute,
  COUNT(DISTINCT ride_id) AS total_rides,
  SUM(meter_reading)     AS total_revenue,
  SUM(passenger_count)   AS total_passengers
FROM streaming_data
GROUP BY minute, timestamp;
```

### Visualize

* In BigQuery results → **Open in Looker Studio**.
* Use `dashboard_sort` for the x-axis (minute timestamps have limitations in LS).
* Build **combo** or **time-series** charts and save the report.

---

## 9) 🧠 Quick decision cheats

* **GUI wrangling (no code)** → **Dataprep**.
* **GUI enterprise integration, hybrid** → **Data Fusion**.
* **Custom Spark/Hadoop batch ETL** → **Dataproc** (clusters).
* **Don’t manage clusters; run Spark** → **Dataproc Serverless** (batch or notebook).
* **Pub/Sub streaming → transform → BQ** → **Dataflow (Beam)**; use **templates** for reuse.
* **Low-latency serving store for streaming features** → **Bigtable**.
* **Must transform/anonymize before warehouse** → **ETL** (Dataproc/Dataflow) not ELT.

---

## ✅ Micro-Checklist (exam cram)

* Define **ETL** vs **ELT** and justify **pre-load transform** cases.
* Dataprep vs Data Fusion: who uses them and where they run.
* Dataproc: clusters vs **Serverless**, workflow templates, storage options, connectors to **BQ/BT**.
* Dataflow: **Beam**, templates, Pub/Sub integration, streaming/batch unification.
* Streaming architecture: **Pub/Sub → Dataflow → BQ/BT**; windowing & exactly-once sinks (conceptual).
* Bigtable fit: **time-series/IoT/low-latency** with careful **row-key** design.
* Labs: know the **Spark template to BQ** (Dataproc Serverless) and **Dataflow template streaming to BQ**, plus **Looker Studio** visualization step.

---

### 👩‍🏫 Teacher’s nudge

When you see **“real-time, events, Pub/Sub, windowing”** think **Dataflow**.
When you see **“Spark code, no cluster ops”** think **Dataproc Serverless**.
And if they say **“transform BEFORE warehouse due to policy/shape”**, pick **ETL** with the right engine, not ELT.
