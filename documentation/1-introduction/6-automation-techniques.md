# 🤖📅 README — Module 03: Automation Techniques (Updated)

**Goal:** Pick the *right* automation style for ETL/ELT pipelines, map it to the correct GCP service, and remember the IAM + “serverless vs not” details the exam loves.

**How to read:** 1) patterns → 2) Cloud Scheduler + Workflows → 3) Cloud Composer → 4) Cloud Run functions → 5) Eventarc → 6) Lab recap (Cloud Run → BigQuery) → 7) Exam cheats.

---

## 1) 🧭 Automation patterns you must recognise

Google Cloud automation splits cleanly into **scheduled** vs **event-driven**.

### A) Time-based (scheduled)

* Run on a defined cadence (hourly, nightly, month-end).
* Typical examples:

    * Scheduled ELT: **BigQuery extract/transform + Dataform + load back to BigQuery**
    * Nightly backfills, periodic Spark batches
* Main services:

    * **Cloud Scheduler** (simple cron trigger)
    * **Workflows** (multi-step orchestration, YAML)
    * **Cloud Composer** (Airflow DAG orchestration)

### B) Event-driven

* Run when *something happens*: file upload, message published, audit log emitted, etc.
* Typical examples:

    * “GCS object finalised → process → load to BigQuery”
    * “BigQuery insert/write event → rebuild dashboard / retrain model”
* Main services:

    * **Cloud Run functions** (serverless code execution)
    * **Eventarc** (routes CloudEvents from many sources to targets)

---

### ✅ Core mapping (keep this in your head)

| Trigger / Requirement                             | Smallest correct tool                               | When it’s not enough                                                 |
| ------------------------------------------------- | --------------------------------------------------- | -------------------------------------------------------------------- |
| “Every day at 01:00 run X”                        | **Cloud Scheduler**                                 | If X is multi-step → **Workflows**                                   |
| “Call A then B with retries/conditions”           | **Workflows**                                       | If it’s a complex DAG (many tasks/sensors/backfills) → **Composer**  |
| “File uploaded to GCS → run code”                 | **Cloud Run function** (triggered via **Eventarc**) | If heavy Spark/cluster work → function calls **Dataproc**            |
| “Trigger on audit/log events (BQ insert/write)”   | **Eventarc** → Cloud Run/Workflows                  | If you need full downstream DAG orchestration → **Composer**         |
| “Complex dependency-rich pipeline across systems” | **Cloud Composer** (Airflow)                        | If only a few API calls, prefer **Workflows** (serverless + simpler) |

> 💡 **Exam Tip**
> Only **Cloud Composer** is *not* serverless here. **Scheduler, Workflows, Cloud Run functions, Eventarc** are serverless.

---

## 2) ⏰ Cloud Scheduler + 🧩 Workflows (lightweight scheduled automation)

### Cloud Scheduler (managed cron)

* Automates tasks by invoking workloads at **recurring intervals**.
* You control **frequency** and **time of day**.
* Triggers: **HTTP/S**, App Engine HTTP, **Pub/Sub**, **Workflows**.
* Common usage: “Scheduled run of a Dataform workflow”.

### Workflows (YAML orchestration)

* A **state machine** to call Google APIs in sequence.
* Supports **branching, retries, conditionals**, and structured multi-step jobs.
* Ideal when you need orchestration but don’t want Airflow.

#### Common exam pattern: Scheduler → Workflows → Dataform API (compile + invoke tagged subset)

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
  - invoke:
      call: http.post
      args:
        url: https://dataform.googleapis.com/v1beta1/projects/${projectId}/locations/${region}/repositories/${repo}:createWorkflowInvocation
        auth: { type: OAuth2 }
        body:
          workflowInvocation:
            compilationResult: ${comp.body.name}
            includedTags: ${tags}
```

> 💡 **Exam Tip**
> “Low coding effort + YAML + scheduled trigger + multi-step API chaining” → **Cloud Scheduler + Workflows**.

---

## 3) 🐍 Cloud Composer (Apache Airflow) — full orchestration

**Cloud Composer** is the **central orchestrator** when workflows span many systems (GCP, on-prem, multicloud).

* Based on **Apache Airflow**
* Core concepts: **operators**, **tasks**, **dependencies**, **DAG (Directed Acyclic Graph)**
* Features: **triggering, monitoring, logging, retries, error handling**, backfills, sensors
* Dev experience: **Python** DAGs

### “Shape” you should recognise

A typical analytics DAG might:

1. pull file from **GCS**
2. load into **BigQuery**
3. run SQL (joins/curation)
4. trigger **Dataproc** for deeper transforms

> 💡 **Exam Tip**
> If the question says “orchestration” + “dependencies / retries / monitoring / backfills” → **Cloud Composer**.

---

## 4) 🧩 Cloud Run functions (serverless event-driven code)

**Cloud Run functions** execute code in response to events.

* Event sources: **HTTP**, **Pub/Sub**, **Cloud Storage**, **Firestore**, and custom events via **Eventarc**
* Multi-language runtime (good for teams with different stacks)
* Best for: “small glue code”, API calls, lightweight transforms, triggering Dataproc/Dataflow, loading into BigQuery

### Event-driven ETL pattern (from transcript)

**GCS upload → Cloud Run function → call Dataproc API → run workflow template → output lands in GCS**

---

## 5) 🛰️ Eventarc (event routing layer)

**Eventarc** enables a unified **event-driven architecture**:

* Connects many event sources (Google Cloud services, third-party, custom via Pub/Sub)
* Targets include **Cloud Run functions**, Workflows, etc.
* Uses **CloudEvents** standard format
* Great for “less frequent / audit-log driven” triggers

### High-yield scenario

* **BigQuery insert/write** generates a **Cloud Audit Log event**
* Eventarc captures it and triggers actions such as:

    * rebuild dashboard
    * retrain ML model
    * run a custom pipeline step

> 💡 **Exam Tip**
> If the trigger is **Audit Logs** (especially BigQuery events) → **Eventarc**.

---

## 6) 🧪 Lab recap — Cloud Run function loads Avro from GCS into BigQuery

### What the lab proves

* **Event-driven ingestion**
* Serverless: file upload triggers compute only when needed
* Uses **BigQuery load job** (good for batch file ingestion)

### Core flow

1. Deploy Cloud Run function (gen2)
2. Trigger on **google.storage.object.finalize**
3. Function loads **Avro → BigQuery table** (autodetect schema)
4. Validate in BigQuery, view logs

### Key code idea (what the exam cares about)

* Event payload provides `bucket` + `name`
* Dataset fixed (e.g., `loadavro`)
* Table derived from filename
* Load options: **AVRO + autodetect + CREATE_IF_NEEDED + WRITE_TRUNCATE**

---

## 7) 🧠 Decision matrix (memorise this vibe)

| Need                                                  | Best pick               |
| ----------------------------------------------------- | ----------------------- |
| Simple cron trigger (HTTP/PubSub)                     | **Cloud Scheduler**     |
| Multi-step API workflow with retries/branching (YAML) | **Workflows**           |
| Complex DAG orchestration across many systems         | **Cloud Composer**      |
| Run code on cloud events (serverless)                 | **Cloud Run functions** |
| Route CloudEvents (incl. audit/log) to targets        | **Eventarc**            |

> 💡 **Exam Tip (from transcript)**
>
> * Cloud Scheduler = **low coding effort** (config-driven)
> * Cloud Composer = **medium effort** (Python DAG)
> * Cloud Run functions = multi-language
> * Eventarc = language-agnostic routing
> * Only Composer is **not serverless**

---

## ✅ Micro-Checklist for the exam

* Identify **scheduled vs event-driven** triggers.
* Scheduler triggers: **HTTP/S**, **Pub/Sub**, **Workflows**.
* Composer = Airflow: **DAG**, operators, tasks, dependencies, retries, monitoring/logging.
* Cloud Run functions: respond to **HTTP / Pub/Sub / GCS / Firestore / Eventarc**.
* Eventarc: **CloudEvents routing**, especially **Audit Log**-driven automation (e.g., BigQuery writes).
* Remember the quiz definitions:

    * **DAG = Directed Acyclic Graph**
    * “Central orchestrator” → **Cloud Composer**
    * “Recurring intervals” → **Cloud Scheduler**
    * “Execute code on events” → **Cloud Run functions**
    * “Unified event-driven architecture” → **Eventarc**

