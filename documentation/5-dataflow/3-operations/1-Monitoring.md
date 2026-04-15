# 🧠 Module — Dataflow Operations: Monitoring & Alerting

This module is about **operating** Dataflow jobs once they are already running.

The key idea is:

> Building a pipeline is not enough. You also need to **monitor it, detect regressions, and alert on unhealthy behaviour**.

---

## 1) What this course section covers

The **Dataflow Operations** course focuses on the operational side of Dataflow:

* monitoring jobs
* using logs and Error Reporting
* troubleshooting pipeline failures
* performance optimisation
* testing and CI/CD
* reliability
* Flex Templates

This specific lesson is mainly about **monitoring and alerting**.

---

## 2) Main monitoring tools

There are **two main places** to monitor Dataflow jobs:

### **Dataflow UI**

Used to inspect:

* job list
* job graph
* job metrics
* custom metrics

### **Cloud Monitoring**

Used to:

* access exported Dataflow metrics
* build custom dashboards
* create alerting policies

**Decision rule:**

* Need to inspect **one job** → use **Dataflow UI**
* Need **dashboards or alerts across jobs** → use **Cloud Monitoring**

---

## 3) Job list page

The Dataflow job list page shows jobs from the **last 30 days**.

You can:

* filter by fields such as status or name
* sort columns
* combine filters with **AND / OR**
* bookmark filtered URLs for repeated use

This is useful for operational views like:

* failed jobs
* running jobs
* all jobs matching a pipeline name

---

## 4) Job Graph page

When you open a job, the first page is the **Job Graph**.

It shows:

### **Job Info panel**

Basic metadata:

* regional endpoint
* worker location
* encryption type
* resource usage
* runtime parameters

### **Pipeline graph**

Visual view of Beam steps.

Important concept:

> Beam steps are not always executed exactly as written.
> Dataflow may **optimise and fuse steps into stages**.

So in the UI:

* multiple Beam steps may share an optimised stage
* the execution view is not always one-to-one with your code

---

## 5) Batch vs streaming in the graph

### **Batch jobs**

* process a finite dataset
* steps typically run **sequentially**
* job eventually completes
* success = green check marks
* failure = red error symbol on failed steps

### **Streaming jobs**

* process unbounded data
* steps/stages run **concurrently**
* job does not finish unless you **cancel** or **drain** it

**Exam trigger:**

* finite + completes → **batch**
* continuous + no natural end → **streaming**

---

## 6) Step-level metrics

When you click a step, you can see:

* optimised stages
* throughput over time
* number of elements
* estimated size
* **wall time**

### **Wall time**

Shows the total worker time spent running that step.

Use it to identify:

* expensive steps
* bottlenecks
* where workers spend most time

---

## 7) Custom metrics in Beam

Beam supports three main metric types:

### **Counter**

Tracks increments/decrements of events or values.

### **Distribution**

Tracks:

* count
* min
* max
* mean

Important:

> It is **not a histogram**.

### **Gauge**

Tracks the **latest value** of something.

These custom metrics appear in the Dataflow UI on the Job Graph page.

---

## 8) Job Metrics page

The **Job Metrics** tab shows time-series graphs.

It differs between **batch** and **streaming** jobs.

---

## 9) Batch job metrics

Main graphs:

### **Autoscaling**

* green line = workers needed
* blue line = current workers

There is a lag because new workers need time to start.

### **Throughput by substep**

Shows how throughput changes over time.
In batch jobs, this often appears in phases because steps do not all run at once.

### **CPU utilisation**

Healthy batch job:

* workers have roughly similar CPU levels

Unhealthy sign:

* a few workers at high CPU, others low

This often means **data skew** or uneven workload distribution.

Example:

* operations like **GroupByKey** cannot be freely split across all workers
* if one key range is much heavier, one worker does most of the work

### **Worker error log count**

Shows how many worker log entries were at error level.

Important batch rule:

> If processing an element fails **four times in a row**, the whole batch job fails.

---

## 10) Streaming job metrics

Streaming jobs also have:

* autoscaling
* throughput
* CPU utilisation
* worker error log count

But the most important extra graphs are:

### **Data freshness**

### **System latency**

These are the key health metrics for streaming jobs.

---

## 11) Data freshness

Data freshness = **difference between real time and the output watermark**.

The output watermark represents a point in event time before which data is **nearly guaranteed** to have been processed.

Example:

* current time = 9:26
* freshness = 6 minutes

This means data with timestamp **9:20 or earlier** has been processed.

### What it tells you

It measures:

> **How far behind real time the pipeline output is**

---

## 12) System latency

System latency measures:

> **How long elements take to travel through the pipeline**

If the pipeline is blocked, latency increases.

### Difference from data freshness

* **Data freshness** = how stale the output is
* **System latency** = how long processing is taking

They often rise together, but they are not the same metric.

---

## 13) Dependency failure pattern

If a downstream sink fails, the pipeline may keep running but stop making real progress.

Example from the transcript:

* pipeline reads from Pub/Sub
* transforms data
* writes to Spanner
* Spanner goes down

Then:

* Pub/Sub does not get confirmation
* messages are not fully cleared
* **system latency rises**
* **data freshness rises**

When the sink recovers:

* backlog is processed
* metrics return toward normal

**Important lesson:**

> A streaming job can be technically “running” but operationally unhealthy.

---

## 14) Pub/Sub input/output metrics

If the streaming job uses Pub/Sub, you may also see:

### **Requests per second**

Rate of API read/write requests.

If it drops sharply or stays near zero:

* there may be no data
* or the pipeline may be blocked

### **Response errors per second by type**

Rate of failed Pub/Sub API requests.

If high:

* inspect the error types
* cross-check Pub/Sub error code docs

---

## 15) Time selector

The UI lets you:

* choose a custom time range
* zoom into a time interval
* inspect full job lifetime

This matters because long-running jobs can hide important behaviour unless you narrow the window.

Example from the transcript:

* the job started **16 hours behind** because data had accumulated before the job started

So always interpret graphs with the selected time range in mind.

---

## 16) Cloud Monitoring and alerting

All Dataflow metrics are exported to **Cloud Monitoring**.

You can use it to:

* explore metrics
* build custom dashboards
* create alerts

Examples:

* alert if `is_failed > 0`
* monitor custom counters
* build dashboards across multiple pipelines

---

## 17) Why alerting matters more in streaming

This is one of the most important lessons.

In streaming:

* failed elements may be retried indefinitely
* the job may **not** move to a failed state
* it can stay “running” while freshness gets worse

So alerting only on **job failure** is not enough.

Better alert targets:

* **system latency**
* **data watermark age / freshness**
* **per-stage watermark age**

**Decision rule:**

* Want to know whether a streaming job is still alive? → status helps
* Want to know whether it is still healthy? → check **freshness and latency**

---

# ✅ Quiz summary

## 1) Backlog of 3 days + sink permission failure: what happens to data freshness?

**Correct answer:**
**Initial start point at 3 days, with an upward sloping line.**

### Why

The subscription already contains **3 days of unprocessed data**, so the job starts 3 days behind.
Because the sink cannot write, the pipeline cannot catch up, so freshness keeps increasing.

---

## 2) Which metrics help monitor whether processed data is still fresh?

**Correct answers:**

* `job/data_watermark_age`
* `job/system_lag`
* `job/per_stage_data_watermark_age`

### Why

These metrics reflect:

* output freshness
* lag through the system
* and where freshness is degrading

### Why not `job/is_failed`

Because a streaming job may be unhealthy **without failing**.

---

# 🔥 Exam cheats

* **Dataflow UI** = inspect individual jobs
* **Cloud Monitoring** = dashboards + alerts
* **Batch jobs finish**; **streaming jobs keep running**
* Dataflow may **fuse Beam steps into optimised stages**
* **Wall time** helps find expensive steps
* **Distribution metric is not a histogram**
* Uneven CPU across workers often means **skew**
* In batch, **4 consecutive element failures** can fail the job
* **Data freshness** = current time minus output watermark
* **System latency** = processing delay through the pipeline
* Streaming jobs can be **running but unhealthy**
* For streaming alerts, monitor **freshness/lag**, not only failure state

---

# One-line takeaway

> **For streaming Dataflow, “job is running” is not the same as “pipeline is healthy.”**
> The real health signals are **freshness, latency, throughput, errors, and workload balance**.