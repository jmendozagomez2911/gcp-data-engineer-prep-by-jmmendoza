# 🧠 Dataflow Reliability

## GCP Data Engineer exam focus

## Core idea

Reliability in Dataflow is not just about making the pipeline work today. It is about knowing:

* what happens when it fails
* how to avoid data loss
* how to recover quickly
* how to design high availability for streaming

### Key point

Most of the important material here is really about **streaming**, not batch.

---

## 1) Batch vs streaming from a reliability perspective

### **Batch**

It is much simpler.

* if it fails, you usually **rerun it**
* source data is not lost
* the sink can often be rewritten

### **Streaming**

It behaves like a long-running application.

* it processes continuously
* it can get stuck
* it can degrade without fully failing
* it needs monitoring, alerting, and recovery planning

### Exam trigger

> **Batch = rerun is often enough**
> **Streaming = you need a real reliability and recovery strategy**

---

## 2) Types of failures

There are two broad groups:

### **1. Failures caused by code or data shape**

* bugs
* corrupted data
* transient errors
* unexpected data formats

### **2. Failures caused by outages**

* service outage
* zonal outage
* regional outage
* network or compute failures

### Important idea

Dataflow sits in the middle of many Google Cloud services, so its reliability also depends on:

* network
* Compute Engine
* Pub/Sub
* BigQuery
* Cloud Storage
* Spanner
* and others

---

## 3) Retries: batch vs streaming

### **Batch**

Failed work items are retried up to **4 times**.
After that, the job fails.

### **Streaming**

Failed work items are retried **indefinitely**.

### Exam point

This is why a streaming pipeline can become **stuck forever** if error handling is poorly designed.

---

## 4) Dead-letter queue: essential

To prevent bad records from breaking or blocking the pipeline:

* use a **dead-letter queue**
* use **error logging**
* isolate bad records

### Recommended pattern

* wrap user logic in `try/catch`
* do not log every single error blindly
* send the bad record to an alternate output
* persist it in BigQuery or Cloud Storage for offline inspection

### What matters

> **Do not let one bad record break or freeze your streaming pipeline.**

---

## 5) Monitoring as part of reliability

Monitoring is not optional. It is part of reliability.

It helps you:

* detect problems before production impact
* track SLIs
* compare them with SLOs
* trigger alerts early

### For batch

You mainly care about:

* total runtime
* whether the job finishes within the expected time window

Typical example:

* a daily batch job
* it must finish before a certain deadline
* you alert if runtime exceeds the allowed threshold

### For streaming

You especially care about:

* **data freshness**
* **system latency**

Because they show whether the pipeline is falling behind.

---

## 6) Location strategy and reliability

### Dataflow is a **regional** service

When you submit a job to a region, Dataflow automatically picks a zone based on capacity, **unless you force a zone**.

### Important rule

**Do not specify a worker zone unless you have a strong reason.**

If you pin a zone:

* you lose flexibility
* you increase exposure to zonal stockouts or zonal issues

### Exam point

If there is a zonal problem, it is often enough to:

* relaunch the job
* **without explicitly specifying the zone**

---

## 7) You cannot move a running job

Once the job has started:

* you cannot change its location

If it is streaming:

* first **drain** or **cancel**
* then relaunch it

This applies whether:

* you change zones inside the same region
* or move to a different region

---

## 8) Geographic design rule

When thinking about location, there are three elements:

* **source**
* **processing**
* **sink**

### Main recommendation

**Keep them in the same region.**

That reduces:

* latency
* cross-region dependencies
* failure surface

### Extra recommendation

When useful, use **multi-regional** options for:

* Cloud Storage
* BigQuery
* Pub/Sub

---

## 9) What to avoid

Avoid architectures with critical dependencies across multiple regions.

Bad example:

* reading from `us-central1`
* writing to `us-east4`

Problem:
if either region fails, the pipeline is affected.

### Exam phrase

> **Critical cross-region dependencies reduce reliability.**

---

# 🧯 Disaster recovery for streaming

## 10) Option 1: Pub/Sub snapshots + Seek

This applies when Pub/Sub is your source.

The idea is:

1. create a subscription snapshot
2. stop and drain the pipeline
3. use **Seek** to roll back to the snapshot state
4. relaunch the pipeline

### Problems

When you do this:

* messages are **reprocessed**
* you may create **duplicates**
* you rerun work that was already done
* windowing and exactly-once logic can become complicated

### Critical point

Dataflow acknowledges a Pub/Sub message when it **reads** it, not when it has already written the final result to the sink.

That matters a lot.

Because it means you may have records that were:

* already read
* already acknowledged
* but not fully materialised in the sink yet

---

## 11) Pub/Sub snapshot retention

The useful retention is limited by Pub/Sub:

* **maximum 7 days**

### Module recommendation

Take snapshots **at least weekly**.

---

## 12) Option 2: Dataflow snapshots

This is the stronger disaster recovery option in the module.

### Advantages over Pub/Sub snapshots + Seek

Dataflow snapshots:

* save the **pipeline state**
* avoid reprocessing in-flight data
* reduce downtime
* reduce recomputation cost
* restore faster

### Best practice

If you use Pub/Sub, create the snapshot **with sources**.

That coordinates:

* pipeline state
* source state

### Flow

1. create a pipeline snapshot
2. drain or stop the pipeline
3. launch a new job from the snapshot

---

## 13) Dataflow snapshot retention

They also have:

* **7 days of retention**

### Recommendation

Schedule coordinated snapshots **at least once a week**, for example with:

* Cloud Scheduler
* Cloud Composer

---

## 14) Important limitation of Dataflow snapshots

Snapshots are **regional**.

That means:

* a job created from a snapshot must be launched in the **same region**
* they help with **zonal outages**
* they do **not** support failover to another region during a **regional outage**

### Highly testable point

> **Dataflow snapshots protect within the same region, not for cross-region failover.**

---

# 🏗️ High availability

## 15) Three factors for HA design

You always need to think about:

### **RTO — Recovery Time Objective**

How much downtime you can tolerate.

### **RPO — Recovery Point Objective**

How much data loss you can tolerate.

### **Cost**

How much you are willing to pay for that reliability.

### Important

There is no universally “best” architecture.
It depends on the balance between:

* downtime
* data loss
* cost

---

## 16) Cheaper HA option: source redundancy

Architecture:

* two subscriptions in different regions
* both read from the same topic
* if one region fails, you start a pipeline in the other

### Advantages

* cheaper than duplicating everything
* reasonably fast recovery
* you can minimise loss if snapshots are coordinated

### Disadvantages

* you may lose intermediate pipeline state from the failed region
* it is not true zero downtime
* downstream systems need a way to switch to the new output

---

## 17) Strongest HA option: duplicate pipelines in parallel

Architecture:

* two active pipelines at the same time
* two different regions
* two different subscriptions
* multi-regional or redundant sinks

### Advantages

* near **zero downtime**
* data loss is very unlikely
* strong protection against regional failure

### Disadvantages

* this is the **most expensive** option
* you duplicate resources across the whole stack
* downstream consumers must know how to switch outputs

### Exam phrase

> **Maximum availability = maximum redundancy = maximum cost.**

---

# 🔥 What you really need to remember

## Most important points

* **Batch** is usually recovered by rerunning it
* **Streaming** needs a real reliability strategy
* Batch has **4 retries**; streaming has **indefinite retries**
* **DLQ** is essential to stop bad records from freezing the pipeline
* **Monitoring + alerting** are part of reliability
* For batch, a strong alert target is **job runtime / elapsed time**
* For streaming, the key health metrics are **data freshness** and **system latency**
* Dataflow is **regional**
* Avoid explicitly pinning a zone if you want protection from zonal stockouts
* Keep **source, processing, and sink in the same region**
* Avoid **critical cross-region dependencies**
* **Pub/Sub snapshots + Seek** can recover the stream, but cause **reprocessing and duplicates**
* **Dataflow snapshots** are better for recovery because they preserve **pipeline state**
* **Snapshot retention = 7 days**
* Dataflow snapshots help with **zonal** recovery, not **regional** failover
* HA design is driven by **RTO, RPO, and cost**
* Redundant sources are cheaper
* Parallel duplicate pipelines are stronger, but much more expensive

---

# 🧪 Interview version

If they ask how you would improve Dataflow reliability, a strong answer would be:

> For batch, I would mainly focus on rerunability and runtime monitoring.
> For streaming, I would design for bad-record isolation with a dead-letter queue, monitor data freshness and system latency, and define alerting against SLOs.
> For disaster recovery, I would prefer Dataflow snapshots over Pub/Sub snapshots plus Seek because they preserve pipeline state and reduce reprocessing.
> For high availability, I would choose the architecture based on RTO, RPO, and cost: either source redundancy for lower cost or duplicated parallel pipelines for near-zero downtime.

---

# Quiz answers

## 1) Which launch command protects against zonal stockouts in `europe-west4`?

The correct one is the command that includes:

* `--region europe-west4`
* **without** `--worker_zone`

Because Dataflow can then choose the best available zone in that region.

## 2) How long is the retention for Dataflow snapshots?

**Seven days**.

---

# One-line takeaway

> In Dataflow, **streaming reliability** depends on error isolation, monitoring, snapshot-based recovery, and the right trade-off between **RTO, RPO, and cost**.