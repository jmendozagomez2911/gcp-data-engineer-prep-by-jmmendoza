# 🧠 Module — Dataflow Operations: Logs, Diagnostics, BigQuery Jobs, and Error Reporting

This module is about **finding the cause of failures quickly** once a Dataflow job is already running or has failed.

The main idea is:

> **Metrics tell you that something is wrong; logs and diagnostics tell you what is wrong.**

---

## 1) Where to look first in Dataflow

At the bottom of the **Job Graph** and **Job Metrics** pages, Dataflow provides a **Logs panel**.

The main tabs are:

### **Job Logs**

These are logs produced by the **Dataflow service** itself.

You can:

* filter by minimum severity
* search by message text

Use this when you want service-level messages about job behaviour.

### **Worker Logs**

These are logs from the **worker VMs**.

You can also:

* filter by severity
* search by message
* narrow logs to a specific **step or substep** by clicking that step in the Job Graph

This is useful when a failure is tied to a particular transform.

### **Diagnostics**

This is the most important tab for fast troubleshooting.

It shows:

* frequently occurring errors over time
* first seen / last seen timestamps
* important **job insights** detected automatically by Dataflow

So instead of reading raw logs first, Diagnostics often gives you the **shortlist of likely causes**.

---

## 2) Why the Diagnostics tab matters

The Diagnostics tab is basically a **triage view**.

It highlights errors such as:

* **worker JAR misconfiguration**
  for missing required classes in the worker JAR

* **JVM crash / worker killed**
  often due to memory pressure or severe worker failure

* **lengthy operation**
  when a step is taking unusually long

* **hot key detected**
  when one key is receiving too much data and creating skew

* **commit key request exceeds size limit**
  in streaming jobs with very large grouped data or too much output from one input element

* **throttling logger worker**
  when log volume is too high and some logs are not sent to Cloud Logging

### Operational lesson

> The Diagnostics tab exists to save you from digging through thousands of log lines.

Example from the transcript:

* a batch job failed
* Diagnostics showed the **JVM crashed due to memory pressure**

So the root cause was visible immediately.

---

## 3) BigQuery Jobs tab

If your pipeline reads from or writes to **BigQuery**, you may also see a **BigQuery Jobs** tab.

This is used for troubleshooting BigQuery activity triggered by the pipeline.

### Requirements

This tab appears when:

* using **Beam 2.24+**
* and you have the **BigQuery Admin** role

### What it shows

It can show BigQuery jobs created by the pipeline, including:

* **extract jobs**
* **query jobs**
* **load jobs**

### What it does **not** show

It does **not** show:

* **streaming inserts**

That is an important quiz point.

---

## 4) How BigQuery I/O behaves in Beam

### Reading from BigQuery

Beam can read BigQuery data in two main ways:

#### **Read whole table**

BigQuery performs an **extract job** and exports data as JSON files to GCS.

#### **Read selected rows**

BigQuery performs a **query job** and exports selected results as JSON files to GCS.

Both of these appear in the **BigQuery Jobs** tab.

### Writing to BigQuery

BigQuery I/O supports:

* **load jobs**
* **streaming inserts**

Default behaviour described in the transcript:

* **bounded PCollections** → usually use **load jobs**
* **unbounded PCollections** → usually use **streaming inserts**

Only **load jobs** appear in the BigQuery Jobs tab.

---

## 5) What the BigQuery Jobs tab is useful for

It helps you inspect:

* which BigQuery jobs ran
* where they ran
* how long they took
* bytes read
* destination URLs
* source tables
* job timeline
* whether a reservation was used

The tab also provides a command you can run with **gcloud** to get more detailed job information.

### Important operational detail

BigQuery jobs run in the **same location as the dataset** they read from or write to.

So if you are troubleshooting BigQuery-related behaviour, **location** matters.

---

## 6) Error Reporting page

The module then moves from job-local logs to **centralised error aggregation**.

### What Error Reporting does

Error Reporting:

* aggregates errors from cloud services
* shows the most frequent or new errors
* groups errors across **all Dataflow jobs in the project**

So the scope is wider than one pipeline.

### What you can do there

You can:

* see how often an error occurred in a time range
* see which jobs experienced it
* inspect the full stack trace
* link it to an external issue tracker
* change the error status to:

    * **open**
    * **acknowledged**
    * **resolved**
    * **muted**

### Core distinction

* **Diagnostics tab** = quick per-job triage inside Dataflow
* **Error Reporting** = central aggregated error view across the project

---

## 7) Practical troubleshooting flow

A good first-pass troubleshooting sequence from this module is:

1. Open the **Logs panel**
2. Check **Diagnostics** first
3. If needed, inspect **Worker Logs**
4. Narrow logs to a specific **step/substep**
5. If BigQuery is involved, inspect the **BigQuery Jobs** tab
6. Use **Error Reporting** to see whether the same error affects multiple jobs

This is the main operational workflow the lesson is teaching.

---

# ✅ Quiz summary

## 1) The BigQuery Jobs tab shows jobs from:

**Correct answers:**

* **Load jobs**
* **Query jobs**

### Why

From the transcript:

* reading a full table uses an **extract job**
* reading selected rows uses a **query job**
* writing bounded data uses **load jobs**
* **streaming inserts do not appear**

Your quiz options did not include **extract jobs** as plain “extract jobs”, only **streaming extracts**, which is not the correct concept here. So among the options given, the right answers are:

* **Load jobs**
* **Query jobs**

---

## 2) Your batch job failed, and the Diagnostic tab shows JVM crash due to memory pressure. What is the best action?

**Correct answer:**

* **Use a larger machine size**

### Why

Memory pressure means the worker does not have enough memory for the workload.

The best direct fix is to increase available memory per worker by choosing a **larger machine type**.

### Why the others are weaker

* **Increase persistent disk size** → disk is not the same as RAM
* **Switch Java to Python** → irrelevant to the immediate failure cause
* **Increase number of machines** → helps parallelism, but does not directly fix per-worker memory exhaustion

---

# 🔥 Exam cheats

* **Job Logs** = Dataflow service logs
* **Worker Logs** = worker VM logs
* **Diagnostics** = fastest view for common failure patterns
* Click a **step/substep** to filter worker logs to that area
* **Diagnostics** can detect hot keys, lengthy operations, JVM crashes, JAR misconfigurations, oversized commit key requests, and log throttling
* **BigQuery Jobs tab** helps troubleshoot BigQuery read/write jobs triggered by the pipeline
* BigQuery reads may use **extract jobs** or **query jobs**
* BigQuery writes for bounded collections usually use **load jobs**
* **Streaming inserts do not show** in the BigQuery Jobs tab
* **Error Reporting** aggregates frequent/new errors across all Dataflow jobs in the project

---

# One-line takeaway

> **For Dataflow incidents, start with Diagnostics, then drill into worker logs, BigQuery jobs, and Error Reporting only as needed.**
