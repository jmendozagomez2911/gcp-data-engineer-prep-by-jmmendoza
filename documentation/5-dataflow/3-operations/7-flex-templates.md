# 🧠 Dataflow Flex Templates

## GCP Data Engineer exam focus

## Core idea

Templates separate:

* **pipeline development**
* **pipeline execution**

This makes Dataflow pipelines easier to:

* reuse
* schedule
* launch by non-developers
* automate

### Key point

For the exam, the most important thing is understanding the difference between:

* **Classic Templates**
* **Flex Templates**

---

## 1) Why templates exist

Normally, when a developer runs a Beam pipeline:

* the SDK stages dependencies to Cloud Storage
* it calls the Dataflow Jobs API
* runtime dependencies must be available

That creates two problems:

* non-technical users may struggle to launch jobs
* scheduling with services like **Cloud Scheduler** becomes less convenient

### What templates solve

Templates let developers prepare the pipeline once, and let other users launch it later without needing the full runtime setup.

---

## 2) Classic Templates

### What they are

With **Classic Templates**:

* the developer stages the pipeline as a **template file**
* the template is stored in **Cloud Storage**
* users launch the pipeline by pointing to that template file

### Main benefit

Users do **not** need runtime dependencies to launch the job.

### Why they matter

They improve:

* automation
* reuse across teams
* operational simplicity

---

## 3) Main limitations of Classic Templates

There are **two major exam-relevant limitations**.

### **1. ValueProvider support limitations**

Classic Templates rely on **ValueProvider** to turn pipeline options into runtime parameters.

Problem:

* not all Beam I/O transforms support ValueProvider
* some open-source I/Os cannot be used properly with Classic Templates

So:

> A pipeline using an I/O without ValueProvider support may not be convertible into a Classic Template.

---

### **2. No dynamic DAG**

This is the most important limitation.

In Classic Templates:

* the graph is built when the template is **created**
* the graph shape cannot change at launch time

### Example

Suppose a pipeline reads from Pub/Sub and, at runtime, the user wants to choose:

* write to **BigQuery**
* or write to **Cloud Storage**

With Classic Templates, that is not dynamically selectable if it changes the graph.

So you would need:

* one template for BigQuery
* another template for Cloud Storage

### Exam phrase

> **Classic Templates do not support dynamic DAG selection at launch time.**

---

## 4) Flex Templates

### What they are

Flex Templates were built to solve Classic Template limitations.

With **Flex Templates**:

* pipeline artifacts are packaged into a **Docker image**
* the image is stored in **Google Container Registry**
* a **metadata spec file** is stored in **Cloud Storage**

Users launch the template by referring to the metadata spec file and passing parameters.

---

## 5) Why Flex Templates are more flexible

The key difference is when the graph is built.

### **Classic Template**

* graph is generated when the template is **created**

### **Flex Template**

* graph is generated when the template is **launched**

That means Flex Templates support more runtime-driven behaviour.

### Key exam point

> **Flex Templates are more flexible because the job graph is created at launch time, not build time.**

---

## 6) How to create a Flex Template

The process is straightforward.

### Step 1: Create `metadata.json`

This file includes:

* template name
* description
* parameter definitions

You can also define:

* **regex validations** for parameters

That helps enforce a **fail-fast** approach, so invalid parameters are rejected before launching the job.

---

### Step 2: Run the build command

Use:

* `gcloud dataflow flex-template build`

This command:

* packages the pipeline artifacts into a **Docker image**
* pushes the image to **Google Container Registry**
* creates a **template spec file** in **Cloud Storage**
* references the metadata file
* includes artifact details such as the JAR and entry point

### Important packaging fact

> **Flex Templates are packaged as Docker images.**

---

## 7) How to launch a Flex Template

Flex Templates can be launched from several places:

* **Google Cloud Console**
* **gcloud**
* **REST API**
* **Cloud Scheduler**

### Important exam detail

Classic and Flex Templates use **different endpoints**.

That is very testable.

---

## 8) Google recommendation

This is one of the clearest takeaways from the module:

> **Google recommends using Flex Templates for any Dataflow pipeline that you want to reuse.**

So unless there is a very specific reason to keep a Classic Template, Flex is usually the preferred option.

---

## 9) Google-provided templates

Google provides many ready-made templates.

### Main idea

You can use them:

* without writing code
* to move data between systems
* with optional simple transformations through **JavaScript UDFs**

Google has also open-sourced these templates on **GitHub**, which is useful for:

* learning Beam best practices
* seeing real template implementations
* contributing improvements

---

## 10) How to identify Classic vs Flex in the UI

In the Dataflow UI:

* **Classic Templates** can show the graph on the right-hand side
* **Flex Templates** do **not** render the graph beforehand

Why?

Because for Flex Templates the final graph may change depending on runtime parameters.

### Quick recognition rule

* graph shown in advance → likely **Classic**
* graph not shown in advance → likely **Flex**

---

## 11) Categories of Google-provided templates

Google-provided templates are classified into:

* **streaming**
* **batch**
* **utility**

### Examples

* **Streaming**: Pub/Sub to BigQuery, Data Masking with DLP
* **Batch**: BigQuery export to Parquet, Spanner export to Cloud Storage
* **Utility**: Streaming Data Generator

### Utility template use case

Useful for:

* proofs of concept
* synthetic data generation
* performance testing

---

# 🔥 What you really need to remember

## Most important points

* Templates separate **development** from **execution**
* **Classic Templates** are stored as template files in **Cloud Storage**
* **Flex Templates** use a **Docker image** plus a **metadata spec file**
* Classic Templates depend on **ValueProvider** support for runtime parameters
* **Classic Templates do not support dynamic DAG changes**
* **Flex Templates generate the graph at launch time**
* **Classic Templates generate the graph at template creation time**
* Flex Templates can be launched via **Console, gcloud, REST API, or Cloud Scheduler**
* Classic and Flex Templates use **different endpoints**
* Google recommends **Flex Templates** for reusable pipelines
* Google-provided templates are grouped into **batch, streaming, and utility**

---

# 🧪 Interview version

If they ask why Flex Templates are preferred, a strong answer would be:

> Flex Templates are preferred because they package the pipeline as a Docker image and generate the job graph at launch time. That makes them more flexible than Classic Templates, especially when the graph or runtime behaviour depends on user parameters. They are also better for reuse, automation, and scheduling.

If they ask for the biggest limitation of Classic Templates, say:

> The main limitation is that the graph is fixed at template creation time, so Classic Templates do not support dynamic DAG selection at runtime.

---

# Quiz answers

## 1) Into which categories are Google-provided templates classified?

**Batch, streaming, and utility**

## 2) How are Flex Templates packaged?

**Docker image**

## 3) Which is a challenge associated with Classic Templates?

**Lack of support for Dynamic DAG (Directed Acyclic Graph)**

---

# One-line takeaway

> **Classic Templates are fixed at build time; Flex Templates are flexible at launch time.**