# 📘✨ **README — Module 03: The Extract, Load, and Transform (ELT) Data Pipeline Pattern**

+ **Goal:** Master ELT on Google Cloud: load first into **BigQuery**, then transform with **SQL/Scripts/Scheduling/Dataform**.
+ **How to use:** Read top-down. Skim the **Exam Tips**. Copy the examples to practise.

---

## 1) 🧭 What is ELT? (mental model + baseline architecture)

**ELT = Extract → Load → Transform** (transform **after** loading into BigQuery).

**Why ELT on GCP**

* ✅ **Simplicity & speed**: get data into BigQuery quickly (staging), transform later.
* ✅ **Scale**: push heavy transforms to BigQuery’s engine.
* ✅ **Choice**: SQL scripts, scheduled queries, Python/Notebooks, **Dataform**.

**Baseline ELT flow**

1. **Extract** from app DBs/files/SaaS → land **structured** data in **BigQuery staging tables**.
2. **Transform in BigQuery**: SQL scripts, UDFs, stored procedures, Dataform workflows.
3. **Publish** to **production tables/views** for BI/ML.

> 💡 **Exam Tip**
> If the prompt says “*load first, transform in BQ*” or “*use BQ’s compute for transforms*”, the answer is **ELT in BigQuery** (often with **Dataform** when workflows get complex).

---

## 2) 🔁 A common ELT pipeline on Google Cloud

* **Ingestion**: EL/replication tools (e.g., `bq load`, BDTS, Datastream→BQ/GCS) write to **staging** tables.
* **Transform**:

    * **SQL** (scripts, UDFs, stored procedures).
    * **Scheduled queries** for recurring jobs.
    * **Python/Notebooks** (BigQuery DataFrames).
    * **Dataform** for full SQL workflow management.
* **Publish**: write to curated **prod** tables or **views** with governance.

---

## 3) 🧾 BigQuery transformation toolkit (scripts, functions, scheduling)

### 3.1 SQL Scripting (procedural)

* Run **multiple statements** with **shared state**.
* Control flow: **`IF`**, **`WHILE`**, **`BEGIN…END`**, **transactions**.
* **Variables**: declare & use system variables.

### 3.2 UDFs (user-defined functions)

* **SQL UDFs** (recommended when possible) or **JavaScript UDFs**.
* JS UDFs can use **external libraries**; community UDFs exist.
* Scope: **temporary** or **persistent**.

### 3.3 Stored Procedures

* Encapsulate logic, **parameterized**, support **transactions**.
* **Apache Spark stored procedures in BigQuery**: author in **PySpark** editor or `CREATE PROCEDURE` with **Python/Java/Scala**; code inline or in **Cloud Storage**.

### 3.4 Remote Functions (Cloud Run)

* Define a **remote function** in BigQuery that calls your **Cloud Run** endpoint (e.g., Python).
* Use it like a UDF in SQL (e.g., compute object lengths from signed GCS URLs).

### 3.5 Notebooks & BigQuery DataFrames

* Explore/transform with **Python** over data larger than RAM.
* Integrates with viz libs; **schedule notebook** executions if needed.

### 3.6 Saved & Scheduled Queries

* **Save**, version, and **share** queries.
* **Schedule** frequency/start/end & result destinations (table or GCS).
* Great for simple pipelines; use **Dataform** when you need dependencies, tests, or post-steps.

> 💡 **Exam Tip**
> “Need to chain many SQL steps + tests + post-actions” → go **Dataform** (not just a scheduled query).

---

## 4) 🧰 **Dataform (Serverless ELT for SQL Workflows)**

**What it is:**
A **serverless framework** embedded in the **BigQuery environment** that allows you to develop, test, document, and **orchestrate SQL-based ELT pipelines**.

---

### **Why use it**

* Centralises **definitions, dependencies, tests (assertions)**, documentation, and **automation** in one place.
* Eliminates the need for **glue code** (for example, code connecting an API to a database) across multiple tools, thereby reducing human error.

---

### **How it runs with BigQuery**

1. You write transformations in **SQLX** or **JavaScript**.
2. Dataform performs **real-time compilation**, dependency validation, and error surfacing.
3. The compiled SQL runs **directly in BigQuery**, either **on demand** or **on a schedule**.

💡 **Clarification:**
When we say that *execution happens inside BigQuery*, it means Dataform doesn’t have its own compute engine. Instead, it **sends your queries to BigQuery’s engine** for execution — just as if you had written and run the SQL manually in the BigQuery console.
Dataform simply **manages the order, scheduling, and testing**, while **BigQuery does the actual processing**.
In other words:

> **Dataform organises and controls the logic; BigQuery executes it.**

---

### **Repository and workspace structure**

* **Workspaces** come with default folders and files.
* Key folders:

  * `definitions/` → `.sqlx` files for **tables, views, incrementals, and declarations**.
  * `includes/` → **JavaScript** helper functions.
* Other files: `.gitignore`, `package.json`, `package-lock.json`, `workflow_settings.yaml`, `README.md`.

---

### **SQLX file structure**

```text
config { ... }          # Metadata, materialisation, tests
js { ... }              # Reusable JS helpers (optional)
pre_operations { ... }  # SQL to run before main body (optional)
-- main SQL body here --
post_operations { ... } # SQL to run after main body (optional)
```

Use helper calls to replace repetitive logic, e.g. `$(mapping.region("country"))`.

---

### **Materialisation types**

* `declaration` → References an existing BigQuery table.
* `table` → Creates or replaces a table from a `SELECT`.
* `incremental` → Creates, then **appends or updates** with new data.
* `view` → Creates or replaces a view (optionally materialised).

---

### **Quality and custom steps**

* **Assertions** (SQL or JS) for enforcing **data quality**.
* **Operations** to execute custom SQL **before, after, or during** pipeline runs.

---

### **Dependencies**

* **Implicit:** use `ref("node_name")` within SQL.
* **Explicit:** define in `config { dependencies: [...] }`.
* **resolve():** reference without creating a dependency.

---

### **Orchestration and graph**

* Visualised as a **DAG** — e.g.
  `customer_source` → `customer_intermediate` → `customer_rowConsistency` → branches to both `customer_ml_training` (operation) **and** `customer_prod_view`.
* **Triggers:**

  * **Internal:** run manually in the UI or schedule within Dataform.
  * **External:** use **Cloud Scheduler** or **Cloud Composer**.
* Execution always takes place **inside BigQuery** (no separate runtime).

---

> 💡 **Exam Tip:**
> If the question mentions *incremental tables, assertions, and ordered dependencies with retries*, the answer is **Dataform**.

---

## 5) 🧪 Lab: Create & execute a SQL workflow in Dataform (recap)

### Task 1 — Create repository

* BigQuery → **Dataform** → **CREATE REPOSITORY**
* **ID:** `quickstart-repository` · **Region:** `REGION`
* Copy the **Dataform service account**.

### Task 2 — Create & init workspace

* Open repo → **CREATE DEVELOPMENT WORKSPACE** → **ID:** `quickstart-workspace` → **INITIALIZE WORKSPACE**

### Task 3 — Define a **view** (`definitions/quickstart-source.sqlx`)

```sql
config { type: "view" }

SELECT "apples" AS fruit, 2 AS count
UNION ALL SELECT "oranges", 5
UNION ALL SELECT "pears", 1
UNION ALL SELECT "bananas", 0
```

### Task 4 — Define a **table** (`definitions/quickstart-table.sqlx`)

```sql
config { type: "table" }

SELECT fruit, SUM(count) AS count
FROM ${ref("quickstart-source")}
GROUP BY 1
```

### Task 5 — Grant IAM to Dataform SA

* **BigQuery Job User**, **BigQuery Data Editor**, **BigQuery Data Viewer**.

### Task 6 — Execute workflow

* Open workspace → **START EXECUTION** → **Execute actions → START EXECUTION**.
* Dataform writes results into dataset **`dataform`**.
* Check **EXECUTIONS** for logs/status.

---

## 6) 🧠 Decision cheats (tool picker)

* **Few steps / simple recurrence** → **Saved + Scheduled Query**.
* **Complex SQL workflow** (deps, tests, post-ops, incremental) → **Dataform**.
* **Custom Python logic** inside SQL → **Remote Function (Cloud Run)** or **JS/SQL UDF**.
* **Reusable parameterized logic** with transactions → **Stored Procedure** (SQL or Spark).
* **Exploration + Python transforms at scale** → **Notebooks + BigQuery DataFrames**.

---

## 7) ✅ Micro-Checklist for the exam

* Know **ELT**: load to **BQ staging**, transform **in BQ**, publish to **prod**.
* **BigQuery scripting**: multi-statement, variables, IF/WHILE, transactions.
* **UDFs**: prefer **SQL**; **JS** for libs/exotic logic.
* **Stored procedures**: SQL & **Spark** (Py/Java/Scala; inline or GCS).
* **Remote functions** (Cloud Run) callable from SQL.
* **Scheduled queries**: automate cadence & destinations.
* **Dataform**: SQLX structure, materializations (table/incremental/view/declaration), **assertions**, **operations**, **dependencies (ref/dependencies/resolve)**, UI & external triggers, runs **in BigQuery**.

---

### 👩‍🏫 Teacher’s nudge

If you can explain **why ELT loads to BigQuery first**, list **all transform options in BigQuery**, and show how **Dataform** turns SQL into a dependable pipeline with **tests & dependencies**, you’ll be in great shape for the exam.
