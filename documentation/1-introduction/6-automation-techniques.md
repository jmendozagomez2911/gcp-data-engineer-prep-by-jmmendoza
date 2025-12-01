# 🤖📅 **README — Module 03: Automation Techniques**

**Goal:** Pick the *right* automation style for your data pipelines (ETL/ELT), wire it up with the correct GCP service, and know the IAM + reliability knobs the exam loves.

**Read me like this:** 1) patterns → 2) Cloud Scheduler & Workflows → 3) Cloud Composer (Airflow) → 4) Cloud Run functions → 5) Eventarc → 6) Hands-on lab (Cloud Run → BigQuery) → 7) Exam cheats + gotchas.

---

## 1) 🧭 Automation patterns you must recognize

### A. **Time-based (scheduled)**

* Run at fixed times (hourly, nightly, monthly closes).
* Tools: **Cloud Scheduler** (simple triggers) → HTTPS / Pub/Sub / **Workflows**; or full DAG with **Cloud Composer**.
* Typical: ELT in BigQuery (**scheduled query / Dataform**), daily backfills (Transfer Service), periodic Spark batches.

### B. **Event-driven**

* React to *something that happens*: file arrives, row inserted, message published, log emitted.
* Tools: **Cloud Run functions** (serverless code) and **Eventarc** (routes CloudEvents from >90 sources).
* Typical: “GCS object finalized → transform → load”, “BQ audit log → rebuild dashboard”, “Pub/Sub message → feature write to Bigtable”.


Keep this mapping in your head:

| Trigger type                                                   | Smallest-correct tool                            | When it’s not enough                                                |
| -------------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------- |
| Every day at 01:00 run X                                       | **Cloud Scheduler** → HTTP / Pub/Sub / Workflows | When you need multi-step orchestration → **Workflows**              |
| Call service A then B with conditionals/retries                | **Workflows** (YAML)                             | Complex cross-env DAG, Airflow operators → **Composer**             |
| File lands in GCS → run code                                   | **Cloud Run function (gen2)** via **Eventarc**   | Heavy Spark step → call **Dataproc** from the function              |
| BigQuery insert/audit event → trigger action                   | **Eventarc** → Cloud Run/Workflows               | If you need downstream DAG → **Composer**                           |
| Long, dependency-rich pipeline across GCS/BQ/Dataproc/Dataflow | **Cloud Composer** (Airflow)                     | If only a few API calls, prefer **Workflows** (cheaper, serverless) |

> **Exam tip:**: Only **Composer** isn’t serverless. Everything else here is.
>
> **Exam tip:**
> * “Cron every day at 2am” → Scheduler.
> * “Call service A then B with retries and conditionals” → Workflows.
> * “Airflow DAG across on-prem + GCP with retries/SLAs” → Composer.
> * “When a file lands in GCS run code” → Cloud Run function (triggered by Eventarc).
> * “Capture BigQuery insert (Audit Log) and fan-out” → Eventarc.
---

## 2) ⏰ Cloud Scheduler + 🧩 Workflows (the lightweight combo)

**Cloud Scheduler** = managed cron. Triggers: HTTPS, App Engine, **Pub/Sub**, **Workflows**.

**Workflows** = YAML state machine to call Google APIs in sequence with branching/retries—perfect when you need a few steps but not a full Airflow cluster.

**Common pattern (Dataform ELT build):** Scheduler → Workflows → Dataform APIs (compile + run a tagged subset).

```yaml
# workflows.yaml (shape)
main:
  params: [projectId, region, repo, tags]
  steps:
  - compile:
      call: http.post
      args:
        url: https://dataform.googleapis.com/v1beta1/projects/${projectId}/locations/${region}/repositories/${repo}:compile
        auth: { type: OAuth2 }
        body: { codeCompilationConfig: { defaultDatabase: projectId } }
      result: comp
  - run:
      call: http.post
      args:
        url: https://dataform.googleapis.com/v1beta1/projects/${projectId}/locations/${region}/repositories/${repo}:createWorkflowInvocation
        auth: { type: OAuth2 }
        body:
          workflowInvocation: { compilationResult: ${comp.body.name}, includedTags: ${tags} }
```

**When to choose:** simple schedules, API chaining, no need for Airflow’s DAG graph.

---

## 3) 🐍 Cloud Composer (Apache Airflow) — full orchestration

* Managed Airflow, Python DAGs, rich **operators** (BigQuery, Dataproc, Dataflow, GCS, Pub/Sub…), retries, SLAs, backfills, sensors.
* Use for **complex** dependency graphs, *many* systems, backfills, conditional branches.

**Mini-DAG you should understand (shape):**

```python
from airflow import DAG
from airflow.providers.google.cloud.transfers.gcs_to_bigquery import GCSToBigQueryOperator
from airflow.providers.google.cloud.operators.bigquery import BigQueryInsertJobOperator
from airflow.providers.google.cloud.operators.dataproc import DataprocSubmitJobOperator
from datetime import datetime

with DAG("analytics_daily", start_date=datetime(2025,1,1), schedule="@daily", catchup=False) as dag:
    gcs_to_bq = GCSToBigQueryOperator(
        task_id="load_raw",
        bucket="raw-bucket",
        source_objects=["sales/{{ ds }}/*.parquet"],
        destination_project_dataset_table="proj.raw.sales",
        source_format="PARQUET", write_disposition="WRITE_APPEND"
    )

    model_sql = """
      CREATE OR REPLACE TABLE proj.curated.sales_day AS
      SELECT * FROM proj.raw.sales WHERE DATE(ts) = DATE('{{ ds }}');
    """
    curate = BigQueryInsertJobOperator(
        task_id="curate", configuration={"query": {"query": model_sql, "useLegacySql": False}}
    )

    spark_job = {"reference": {"project_id": "proj"},
                 "placement": {"cluster_name": "etl-cluster"},
                 "pysparkJob": {"mainPythonFileUri": "gs://jobs/feature_gen.py"}}
    features = DataprocSubmitJobOperator(task_id="features", job=spark_job, region="REGION")

    gcs_to_bq >> curate >> features
```

> **Exam tip:** “Backfill last 7 days only when upstream succeeded,” “cross-cloud + on-prem,” “fine-grained retries/SLAs” → **Composer**.

---

## 4) 🧩 Cloud Run functions (serverless glue)

* Execute small units of code on events: **HTTP**, **Pub/Sub**, **GCS**, **Firestore**, custom via **Eventarc**.
* Great for **file-driven** loads, quick API calls, small transforms, low ops.

### Lab code explained (loads Avro → BigQuery)

```js
const {Storage} = require('@google-cloud/storage');
const {BigQuery} = require('@google-cloud/bigquery');
const storage = new Storage();
const bigquery = new BigQuery();

exports.loadBigQueryFromAvro = async (event) => {
  // 1) Event payload from GCS (“object finalized”) has bucket + name
  const bucketName = event.bucket;         // gs://<bucket>
  const fileName = event.name;             // e.g. campaigns.avro

  // 2) Decide BQ dataset/table (table = file name without .avro)
  const datasetId = 'loadavro';
  const tableId = fileName.replace('.avro','');

  // 3) Load options — let BQ infer schema from Avro, create if needed, overwrite table
  const options = {
    sourceFormat: 'AVRO',
    autodetect: true,
    createDisposition: 'CREATE_IF_NEEDED',
    writeDisposition: 'WRITE_TRUNCATE'
  };

  // 4) Kick off server-side load: GCS object → BigQuery table
  await bigquery.dataset(datasetId).table(tableId)
    .load(storage.bucket(bucketName).file(fileName), options);

  // If successful, the table exists/updated and can be queried immediately.
};
```

**Why this pattern matters for the exam:** It demonstrates **event-driven ingestion**, idempotent loads (per object), least-ops, and separation of storage (GCS) from warehouse (BQ).

**IAM you typically need**

* Function’s **service account**: `roles/bigquery.dataEditor` (or table-scoped) + `roles/bigquery.jobUser`.
* Function **trigger**: Eventarc wiring from **GCS → Cloud Run function** (grants for event receiver + GCS service agent to Pub/Sub).

---

## 5) 🛰️ Eventarc (wire events to targets)

* **Routes CloudEvents** from many sources (GCS, BQ Audit Logs, Pub/Sub, Firebase, custom) to **targets** (Cloud Run, Cloud Functions, Workflows, GKE).
* You filter by event type & attributes; **language-agnostic** (only the target matters).

**Useful mental model scenarios**

* “Whenever a **BigQuery table** gets new rows (Audit Log: `InsertJob` writing to table), **trigger** a recompute (Cloud Run/Workflows).”
* “When **GCS** file arrives under `/landing/2025/` only, **invoke** a specific transform.”

> **Exam tip:** If you see “**on insert/update** trigger X” or “**audit-log** driven action,” it’s **Eventarc → Cloud Run**.

---

## 6) 🧪 Lab recap — Cloud Run function → BigQuery (end-to-end)

1. **Enable/Configure**

```bash
export PROJECT_ID=$(gcloud config get-value project)
export REGION=REGION
gcloud config set run/region $REGION
gcloud config set eventarc/location $REGION
gcloud services enable run.googleapis.com eventarc.googleapis.com cloudfunctions.googleapis.com pubsub.googleapis.com logging.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
```

2. **Permissions**

```bash
export PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:$PROJECT_NUMBER-compute@developer.gserviceaccount.com" \
  --role="roles/eventarc.eventReceiver"

SERVICE_ACCOUNT="$(gcloud storage service-agent --project=$PROJECT_ID)"
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/pubsub.publisher"
```

3. **Create resources + deploy**

```bash
bq mk -d loadavro
gcloud storage buckets create gs://$PROJECT_ID --location=$REGION

# code (index.js) + deps
npm install @google-cloud/storage @google-cloud/bigquery
gcloud functions deploy loadBigQueryFromAvro \
  --gen2 --runtime nodejs20 --source . --region $REGION \
  --trigger-resource gs://$PROJECT_ID \
  --trigger-event google.storage.object.finalize \
  --memory=512Mi --timeout=540s \
  --service-account=$PROJECT_NUMBER-compute@developer.gserviceaccount.com
```

4. **Test + verify**

```bash
wget https://storage.googleapis.com/cloud-training/dataengineering/lab_assets/idegc/campaigns.avro
gcloud storage cp campaigns.avro gs://$PROJECT_ID        # triggers the function
bq query --use_legacy_sql=false 'SELECT * FROM `loadavro.campaigns` LIMIT 10;'
gcloud logging read "resource.labels.service_name=loadBigQueryFromAvro" --limit=10
```

**Reliability tips (interview/exam-style)**

* **Idempotency:** guard against duplicate GCS events (check if load job/table version already processed—store processed file names in a meta table).
* **Back-pressure:** for bursts, prefer **BQ Load Jobs** (like the lab) over streaming inserts.
* **Least privilege:** restrict dataset/table IAM; use per-function service accounts.

---

## 7) 🧠 Decision matrix (memorize this vibe)

| Need                                           | Best pick                            |
| ---------------------------------------------- | ------------------------------------ |
| Single HTTP/PubSub trigger on a schedule       | **Cloud Scheduler**                  |
| A few API calls with retries/branching         | **Workflows** (optionally scheduled) |
| Complex DAGs, dependencies, sensors, backfills | **Cloud Composer (Airflow)**         |
| Lightweight code on events, serverless         | **Cloud Run functions**              |
| Route audit/log/object events to targets       | **Eventarc** (+ Cloud Run target)    |

---

## ✅ Micro-Checklist (exam cram)

* Distinguish **scheduled vs event-driven** automation.
* Pick **Composer** for complex orchestration; **Workflows** for small multi-step jobs.
* Know **Cloud Run function** triggers (HTTP / Pub/Sub / GCS / Firestore via **Eventarc**).
* IAM basics: **eventReceiver**, **pubsub.publisher**, **bigquery.jobUser**, table-scoped editors.
* Prefer **BQ Load jobs** for file ingestion; remember **autodetect from Avro**.
* Reliability: retries, idempotency, regional alignment, dead-letter topics (for Pub/Sub flows).

---

### 👩‍🏫 Teacher’s nudge

If it’s **cron-like** → Scheduler/Workflows.
If it’s **complex DAGs** → Composer.
If it’s **“when X happens, run code”** → Cloud Run + Eventarc.
If you can explain that mapping—and quote a couple IAM roles—you’re answering like a certified Data Engineer.
