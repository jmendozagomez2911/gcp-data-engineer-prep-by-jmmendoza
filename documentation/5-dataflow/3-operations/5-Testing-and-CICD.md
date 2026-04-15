
# Dataflow CI/CD, Testing, Deployment, Updates, Drain vs Cancel

## 1. Main idea of the module

This module explains how to manage the **full lifecycle** of a Dataflow pipeline safely:

* how to **test** it
* how to **build and package** it
* how to **deploy** it
* how to **update** a running streaming job
* how to **roll back**
* how to **terminate** it correctly

The key message is that Dataflow pipelines are not just normal applications. They often keep **state** over time, especially in streaming, so changing them is more delicate than changing a stateless service.

---

## 2. Why CI/CD is more sensitive in Dataflow

In normal software, a bad deployment is already annoying. In Dataflow, it can be worse because:

* pipelines may keep **state**
* pipelines may perform **non-idempotent side effects** on external systems
* changing logic or topology may break compatibility with the running job
* rollback may still leave side effects that already happened

So the module pushes a safe mindset:

* test thoroughly
* deploy in a controlled way
* validate changes
* always have a rollback strategy

---

## 3. Testing strategy in Beam / Dataflow

The module presents testing as a layered strategy.

### Unit tests

These test small pieces of logic, usually:

* individual **DoFns**
* individual **PTransforms**
* small pipeline fragments

They should:

* run quickly
* run locally
* avoid external systems

In Beam Java, the important tools are:

* **JUnit 4**
* **TestPipeline** instead of `Pipeline.create()`
* **PAssert** to validate `PCollection` contents

### Integration tests

These use **real I/O systems**, but only with a **small amount of test data**.

Purpose:

* verify that the pipeline interacts correctly with actual sources and sinks

### End-to-end tests

These use a **larger, production-like dataset** and validate the whole behaviour of the pipeline in a more realistic environment.

---

## 4. Direct Runner vs production runner

The module makes this distinction very clear:

### Direct Runner

Use it for:

* local development
* unit tests
* small integration tests

### Dataflow runner / production runner

Use it for:

* larger integration tests
* performance testing
* deployment testing
* rollback testing

So the Direct Runner is for fast local confidence, while Dataflow is for realistic validation.

---

## 5. Important unit-testing advice

## Use named DoFn classes, not anonymous subclasses

This is one of the most exam-relevant points.

Anonymous subclasses are considered an **anti-pattern** because they are **harder to test**. If the logic is embedded inline, you often end up duplicating logic in tests, which becomes messy and hard to maintain.

Named subclasses are better because:

* they are reusable
* they are testable independently
* they keep the pipeline cleaner

This is not about performance. It is about **testability and maintainability**.

---

## 6. Testing windows and streaming behaviour

The module also highlights that you should test **windowing logic**, not only basic transforms.

### For window testing

Use timestamped test data so you can verify how elements fall into windows.

### For streaming tests

Use **TestStream**.

`TestStream` lets you simulate:

* unbounded input
* timestamps
* watermark movement
* processing time progression

That is important because streaming behaviour depends on **time**, not just on values.

Also, `TestStream` is supported by:

* Direct Runner
* Dataflow Runner

---

## 7. Integration testing patterns

For larger tests, the recommendation is to work in a **non-production environment** but use data that is close to production.

Examples from the module:

* copy Cloud Storage data with **Storage Transfer Service**
* copy or reuse BigQuery datasets carefully
* for Pub/Sub, attach an additional subscription to the same production topic

This last point is useful because it allows:

* realistic streaming tests
* cloning production traffic patterns
* even **A/B testing** in some cases

---

## 8. Artifact building

For Java pipelines, build artifacts are typically managed with:

* **Maven**
* **Gradle**

Beam artifacts are available from **Maven Central**.

Important note from the transcript:

* you usually need more than Beam core
* you often also need dependencies for:

    * the **Dataflow runner**
    * **Google Cloud I/O connectors**

The module also says Beam uses **semantic versioning** and recommends **Beam 2.26+**, because those versions use the **Google Cloud libraries BOM**, which helps reduce dependency conflicts.

---

## 9. Deployment options

The module explains **two ways to deploy** a Dataflow job.

### Direct launch

You run the pipeline from the development environment itself.

Examples:

* Java: from Maven or Gradle
* Python: run the script directly

### Templates

Templates let you launch a pipeline **without requiring a developer environment**.

Benefits:

* easier automation
* better separation between build and execution
* easier for schedulers like **Airflow**
* safer for non-technical users

Important detail:

* **Dataflow SQL** is basically a special case built on top of a **Flex Template**

---

## 10. Streaming pipeline updates

This is one of the most important parts.

Streaming jobs are long-running and keep state, so updates must preserve compatibility.

To update a job, you must:

* submit the new pipeline with the **update** option
* use the same **jobName** as the existing pipeline
* provide **transformNameMapping** if transform names changed

Why does this matter?

Because Dataflow needs to match the old pipeline state with the new pipeline graph.

---

## 11. Snapshot concept

Before updating a streaming job, the module strongly recommends taking a **snapshot**.

A snapshot captures the intermediate state of the running pipeline. It is useful for:

* rollback
* update validation
* backup and recovery
* migration to **Streaming Engine**

Important details:

* snapshots can be created in the UI or CLI/API
* if Pub/Sub is used, creating a snapshot **with source** can coordinate unread Pub/Sub messages with pipeline state
* the pipeline pauses briefly during snapshot creation
* this may take minutes, depending on buffered state
* snapshots are better taken during low-impact periods

Very important limitation:

* jobs created from snapshots **must run with Streaming Engine enabled**
* they cannot later be run with Streaming Engine disabled

---

## 12. Update compatibility failures

Not every change is compatible with an in-place streaming update.

The compatibility check may fail if you:

* rename/remove transforms without providing mapping
* add or remove side inputs
* change coders
* change region or zone
* remove stateful operations inside fused steps

If that happens, the module recommends:

* **drain** the old pipeline
* then launch a **new job**

---

## 13. Drain vs Cancel

This is a classic exam topic.

### Drain

Drain is only for **streaming pipelines**.

What happens:

* the job stops reading new data from the source
* already buffered / in-flight data continues processing
* workers shut down only after that data finishes

But there is an important catch:

* the watermark is moved to **infinity**
* this closes windows
* this may produce **incomplete aggregations**

So drain avoids record loss, but it may still create partial window results.

The module suggests using **PaneInfo** if you need to identify incomplete windows and handle them separately.

### Cancel

Cancel can be used for **batch and streaming**.

What happens:

* ingestion stops immediately
* processing stops immediately
* in-flight data may be lost

So cancel is appropriate only if your use case tolerates data loss or if you can replay the source later.

---

## 14. Practical lifecycle for a streaming pipeline

The module’s logic is:

### First deployment

No existing state exists, so just deploy.

### Updating an existing streaming job

* take a snapshot first
* update the job
* provide transform mappings if names changed
* let Dataflow run the compatibility check

### If update is incompatible

Choose between:

* **cancel + replay**, if the source can be replayed
* **drain + relaunch**, if replay is not possible

So the real decision depends on whether you can afford:

* **data loss**
* **reprocessing**
* **incomplete aggregations**

---

# Quiz answers

## 1. Using anonymous subclasses in your ParDos is an anti-pattern because:

**Correct answer:**

* **Anonymous subclasses are harder to test than concrete subclasses.**

### Why

The transcript explicitly says anonymous subclasses make testing harder because you often need to duplicate the logic in tests.

---

## 2. When draining a streaming pipeline, what should you expect to happen?

**Correct answer:**

* **Ingestion stops immediately, windows are closed, and processing of in-flight elements will be allowed to complete.**

### Why

That is exactly the behaviour described:

* the pipeline stops pulling from the source
* the watermark moves to infinity
* windows are closed
* buffered/in-flight elements continue being processed

It is **not** the same as cancelling, because cancel stops processing immediately and can lose data.

---

# What you should remember for the exam

## High-value points

* Use **unit, integration, and end-to-end tests**
* Use **TestPipeline** and **PAssert** for Beam unit tests
* Prefer **named DoFn subclasses** over anonymous ones
* Use **TestStream** for streaming tests
* Use **Direct Runner** locally, **Dataflow runner** for realistic larger tests
* Use **templates** for safer and more automatable deployments
* Take a **snapshot before updating** a streaming pipeline
* Streaming updates require **compatibility**
* If transform names changed, provide **transformNameMapping**
* **Drain** preserves in-flight processing but may create incomplete aggregations
* **Cancel** stops everything immediately and may lose data
