# 🧠 Module — Dataflow Operations: Troubleshooting & Debugging Workflow

This module is about **how to debug a Dataflow job systematically** instead of guessing.

The core workflow is:

> **1. Check for errors**
> **2. Check Job Metrics for anomalies**

That is the main operational pattern the module wants you to remember.

---

## 1) General troubleshooting workflow

### Step 1: Check for errors

Start from the **Dataflow Jobs page**:

* look at the **job status**
* if it is **failed**, open the job
* inspect the **error notification** above the job graph
* look for the **failed step**
* expand the **Logs** section
* open full logs in **Cloud Logging** if needed

Important point:

> A problematic job is not always in **Failed** state.
> Some unhealthy jobs may still be in **Running** state.

This matters especially for streaming.

---

### Step 2: Check Job Metrics for anomalies

The most useful signals are:

#### **Data freshness**

For streaming jobs, rising data freshness means:

> workers are **falling behind input**

#### **System latency**

For streaming jobs, rising system latency means:

> some work item is taking too long to be processed

#### **CPU utilisation**

For all jobs, CPU helps diagnose:

* whether the job is **CPU-bound**
* whether work is **well parallelised**

Example of unhealthy pattern:

* one or a few workers high CPU
* others near zero

That usually means **limited parallelism** or **skewed work distribution**.

---

## 2) Four main classes of Dataflow problems

The module groups pipeline troubles into **four categories**.

---

### A. Pipeline construction / graph-building errors

These happen while Beam is **building the pipeline graph**, before Dataflow even runs the job.

Typical causes:

* illegal pipeline operations
* invalid Beam usage
* incorrect input/output specifications

Key characteristics:

* often reproducible with the **Direct Runner**
* can often be caught with **unit tests**
* **no Dataflow job is created**
* error usually appears in the **console / terminal**

### Important distinction

These are **Beam-side** problems, not Dataflow service runtime problems.

---

### B. Dataflow service validation errors

These happen **after** Beam constructs the graph and sends it to Dataflow.

Then Dataflow validates things such as:

* access to staging/temp **Cloud Storage buckets**
* required **IAM permissions**
* access to input/output sources

Typical example:

* the pipeline code is valid
* but Dataflow rejects the job due to **missing permissions**

Key characteristics:

* not reproducible with the **Direct Runner**
* require the **Dataflow runner / service**
* error appears in:

    * Dataflow monitoring UI
    * and often the terminal if running in blocking mode

### Best practice

Because these errors do not depend on scale:

> test with a **small pipeline or tiny input** to iterate cheaply and catch regressions fast.

---

### C. Exceptions during pipeline execution

These happen while the job is already running, usually inside **user code** such as `DoFn`s.

Typical cause:

* unhandled exception in worker code

How to investigate:

* use the normal troubleshooting workflow
* inspect logs
* open Cloud Logging for full stack traces

### Defensive techniques

The module recommends guarding your code:

* catch exceptions inside `DoFn`
* drop bad elements if appropriate
* send bad elements to a **side output**
* log failing elements for later inspection

### Ways to track failing elements

* log them to Cloud Logging
* inspect worker / startup logs
* write them to an additional output collection

---

### D. Slow pipelines / lack of output / performance problems

Not all problems are hard failures.

A pipeline may be:

* too slow
* blocked
* producing too little output
* spending too much time in one step

The UI helps you inspect step-level performance.

Useful step metrics:

* **wall time**
* input elements
* input bytes
* output elements
* output bytes

### Wall time

Wall time is especially important.

It is the approximate total time spent across all workers/threads on:

* step initialisation
* data processing
* shuffling
* ending the step

Use it to identify **expensive steps**.

---

## 3) Batch vs streaming exception behaviour

This is a high-yield distinction.

### Batch

If a task fails, Dataflow retries it up to **4 times**.

So batch jobs often fail visibly after repeated retries.

### Streaming

A failed streaming job may **stall indefinitely** rather than fail cleanly.

So for streaming, you often need extra signals:

* data freshness
* logs
* Cloud Monitoring
* progress metrics
* error counts

### Exam lesson

> **Batch tends to fail. Streaming tends to stall.**

That is the mental shortcut.

---

## 4) Practical debugging rules

### If the pipeline never starts

Think:

* graph construction error
* or validation error

Ask:

* Was a Dataflow job actually created?

    * **No** → likely graph construction
    * **Yes, but rejected** → likely Dataflow validation

---

### If the job fails during execution

Think:

* unhandled exception in worker code
* inspect worker logs and stack trace

---

### If the job runs but output is missing or slow

Think:

* performance issue
* blocked sink
* skew
* lack of parallelism

Then inspect:

* wall time
* CPU utilisation
* step input/output counts
* freshness / latency for streaming

---

## 5) Quiz summary

### 1) Which two statements are true for failures while building the pipeline?

**Correct answers:**

* **The failure can be caused by incorrect input/output specifications**
* **The failure is reproducible with the Direct Runner**

### Why

Graph-building errors happen on the **Beam side**, before Dataflow service execution.

So:

* bad I/O specs can trigger them
* Direct Runner can often reproduce them

### Why the others are wrong

* **Insufficient controller service account permissions** is usually a **Dataflow validation** issue, not graph construction
* **The error message is visible in Dataflow** is wrong because **no job may be created at all**

---

### 2) Your Dataflow batch job fails after running for close to 5 hours. Which two steps would you take?

**Correct answers:**

* **Investigate the wall time of the individual steps in the job**
* **Check the Dataflow worker logs for warnings or errors related to work item failures**

### Why

For a long-running batch failure, you want:

* step-level performance clues (**wall time**)
* concrete execution errors from workers (**worker logs**)

### Why the others are weaker

* **Log the failing elements** can help, but after a 5-hour failure it is not the first universal troubleshooting step unless you already suspect bad records
* **Data freshness and system latency** are mainly **streaming** health metrics, not the main tools for a batch failure

---

# 🔥 Exam cheats

* Troubleshooting workflow = **errors first, metrics second**
* A job can be unhealthy even if still **Running**
* **Graph construction errors** happen before job creation
* **Validation errors** happen after graph submission to Dataflow
* **Direct Runner** helps reproduce Beam-side construction problems
* **IAM / bucket access issues** are usually Dataflow validation problems
* **User-code exceptions** happen during execution and show in logs / stack traces
* **Batch retries up to 4 times**
* **Streaming may stall indefinitely**
* **Wall time** is a key metric for finding expensive steps
* CPU imbalance often means **limited parallelism or skew**

---

# One-line takeaway

> **Debug Dataflow in layers: first identify whether the problem is build-time, validation-time, runtime, or performance-related; then use logs and metrics that match that layer.**
