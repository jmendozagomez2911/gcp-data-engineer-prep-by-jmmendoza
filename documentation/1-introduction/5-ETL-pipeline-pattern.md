# 🛠️⚡ README — Module 03: ETL Data Pipeline Pattern (Google Cloud)

## Goal of this module

Understand **ETL (Extract → Transform → Load)** in Google Cloud, the **main ETL tools**, and how **batch vs streaming** pipelines land in **BigQuery** (analytics) or **Bigtable** (low-latency serving).

---

## 1) 🧭 What ETL means (baseline architecture)

**ETL = you transform the data *before* it lands in the destination store.**
So the **transformation happens on a processing engine** (GUI tool runner / Spark / Beam), and then the output is **loaded** into BigQuery or Bigtable.

**Baseline flow**

1. **Extract**: databases, files, SaaS apps, event streams
2. **Transform**: run on a processing service (visual tools, Spark, Beam)
3. **Load**: write results to **BigQuery** (analytics) and/or **Bigtable** (serving)

**When ETL is the right answer**

* You must **clean/mask/anonymise/validate** before data is stored in the warehouse.
* The transformation is **heavy / non-SQL / needs Spark/Beam**.
* You want a repeatable pipeline that always produces “curated” output.

---

## 2) 🧑‍🎨 GUI tools (visual / low-code ETL)

### 🧼 Dataprep (Trifacta/Alteryx-style “recipes”)

Think of Dataprep as: **“visual data cleaning and preparation”**.

* You connect to sources and build a **recipe**: a chain of transformations (split, replace, extract, dedupe, etc.).
* You can preview the effect before running.
* When you execute, the transformation can run as a job (the module describes it running with Dataflow in the background).

*(High-yield memory)*: **Dataprep = analyst-friendly wrangling + recipes + visual preview**.
(Training/labs still present it as a Google Cloud tool for no-code data prep.) ([Google Cloud][1])

### 🔗 Cloud Data Fusion (enterprise integration, drag-and-drop pipelines)

Think of Data Fusion as: **“visual pipeline builder for many systems (including on-prem)”**.

* Drag-and-drop **sources → transforms → sinks**.
* Built on **CDAP** (open-source) and supports many connectors/plugins. ([Google Cloud][2])
* It can execute pipelines using managed runtimes, including creating **ephemeral Dataproc clusters** in some setups. ([Google Cloud Documentation][3])

*(High-yield memory)*: **Data Fusion = enterprise integration + hybrid (on-prem + cloud) + plugins**. ([Google Cloud][2])

> **Exam rule of thumb**
>
> * “No-code wrangling / recipes / data cleaning preview” → **Dataprep**
> * “Enterprise integration, lots of connectors, hybrid/on-prem, drag-and-drop pipelines” → **Data Fusion**

---

## 3) 🐘 Dataproc (managed Hadoop/Spark) — batch ETL

Dataproc is: **managed Hadoop/Spark** on Google Cloud when you want the open-source ecosystem.

Use it when:

* you already have Spark/Hadoop jobs (lift-and-shift),
* you need full control of cluster behaviour (versions, init actions, tuning),
* you want batch ETL with Spark and connectors to sinks like BigQuery / Bigtable.

**Workflow Templates**
Dataproc supports workflow templates to define **multi-step jobs + dependencies** (often described as YAML-defined workflows submitted via CLI).

---

## 4) 🚀 Dataproc Serverless for Spark (Spark without managing clusters)

This is the “Spark but I don’t want clusters” option.

* You submit Spark work and Google manages the infrastructure.
* Two execution modes:

  * **Batch workloads**
  * **Interactive sessions** (notebooks/dev exploration) ([Google Cloud Documentation][4])
* Pricing is **pay-per-execution** (billed per second with minimums), so it’s good for bursty workloads. ([Google Cloud][5])

*(High-yield memory)*: **Dataproc Serverless = Spark jobs, no cluster ops, batch + interactive**. ([Google Cloud Documentation][4])

---

## 5) 🌊 Streaming ETL on Google Cloud (Pub/Sub + Dataflow)

### Batch vs Streaming

* **Batch**: fixed dataset processed periodically (payroll, billing)
* **Streaming**: continuous events (fraud detection, clickstream, IoT)

### Core streaming ETL pattern

1. **Ingest** events into **Pub/Sub** (acts like a scalable event hub)
2. **Process** in real time with **Dataflow**
3. **Load** results into:

  * **BigQuery** for analytics
  * and/or **Bigtable** for low-latency serving

### Dataflow essentials

Dataflow is Google’s managed runner for **Apache Beam** (batch + streaming in one model).
Templates are a big exam concept:

* **Templates separate design from deployment** (dev builds template; others deploy later). ([Google Cloud Documentation][6])
* Templates are **parameterised**, so the same pipeline can be reused with different inputs/outputs. ([Google Cloud Documentation][6])
* You can deploy templates from Console, CLI, or API. ([Google Cloud Documentation][6])

*(High-yield memory)*: **Streaming keywords “Pub/Sub + Beam + Dataflow + templates” → Dataflow**. ([Google Cloud Documentation][6])

---

## 6) 🧱 Bigtable’s role in pipelines

Bigtable is a **wide-column NoSQL** database designed for **very low latency and high throughput**. It’s commonly used as the “serving store” in streaming architectures when you need fast lookups. ([Google Cloud Documentation][7])

Key ideas (high-yield):

* Bigtable is ideal for **large amounts of single-keyed data** with low latency. ([Google Cloud Documentation][7])
* The **row key is the primary index** (design it based on access patterns). ([Google Cloud Documentation][8])
* Typical fit: time series, IoT telemetry, clickstream, financial ticks, feature serving. ([Google Cloud][9])

> **Exam rule of thumb**
>
> * “analytics warehouse” → **BigQuery**
> * “millisecond serving / fast key lookups / time-series at scale” → **Bigtable** ([Google Cloud Documentation][7])

---

## 7) 🧪 Lab recap — Serverless Spark → BigQuery (batch)

Core point: you used **Dataproc Serverless batch** to run a Spark job/template that reads an Avro file from **GCS** and loads a table into **BigQuery** (BigQuery connector jar involved).

*(This matches the module’s message: Spark execution happens in Serverless Spark; BigQuery is the destination.)* ([Google Cloud Documentation][4])

---

## 8) 🧪 Lab recap — Streaming pipeline Dataflow → BigQuery → Looker Studio

Core point: you created a **Dataflow streaming job from a template**, wrote to **BigQuery**, then visualised results in Looker Studio.
The reusable/parameterised part is the **template**. ([Google Cloud Documentation][6])

---

## 9) 🧠 Exam cheats (the fast decision map)

* **Dataprep** → visual **data wrangling recipes** (no/low code), preview-first ([Google Cloud][1])
* **Data Fusion** → enterprise **integration pipelines**, hybrid/on-prem, plugins (CDAP) ([Google Cloud][2])
* **Dataproc** → managed **Spark/Hadoop clusters** (control + open-source ecosystem)
* **Dataproc Serverless for Spark** → **Spark without cluster management**, batch + interactive ([Google Cloud Documentation][4])
* **Dataflow** → serverless **Beam** for batch/streaming; **templates** = reuse + parameters ([Google Cloud Documentation][6])
* **Bigtable** → low-latency wide-column NoSQL serving store ([Google Cloud Documentation][7])

---

If you want, I can also rewrite this into an even more “quiz-optimised” version (shorter, with only the phrases that typically appear in questions), but this version should already fix the confusion you had about **what runs where** and **why each tool exists**.

[1]: https://cloud.google.com/blog/products/gcp/google-cloud-dataprep-is-now-a-public-beta?utm_source=chatgpt.com "Google Cloud Dataprep is now a public beta"
[2]: https://cloud.google.com/data-fusion?utm_source=chatgpt.com "Cloud Data Fusion"
[3]: https://docs.cloud.google.com/data-fusion/docs/release-notes?utm_source=chatgpt.com "Cloud Data Fusion release notes"
[4]: https://docs.cloud.google.com/dataproc-serverless/docs/overview?utm_source=chatgpt.com "Serverless for Apache Spark overview"
[5]: https://cloud.google.com/dataproc-serverless/pricing?utm_source=chatgpt.com "Serverless for Apache Spark pricing - Dataproc"
[6]: https://docs.cloud.google.com/dataflow/docs/concepts/dataflow-templates?utm_source=chatgpt.com "Dataflow templates"
[7]: https://docs.cloud.google.com/bigtable/docs/overview?utm_source=chatgpt.com "Bigtable overview"
[8]: https://docs.cloud.google.com/bigtable/docs/schema-design?utm_source=chatgpt.com "Schema design best practices | Bigtable"
[9]: https://cloud.google.com/bigtable?utm_source=chatgpt.com "Bigtable: Fast, Flexible NoSQL"
