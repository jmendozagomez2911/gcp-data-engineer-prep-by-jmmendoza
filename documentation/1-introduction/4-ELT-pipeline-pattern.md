# 📘✨ **README — Module 03: The Extract, Load, and Transform (ELT) Data Pipeline Pattern**

* **Goal:** Master ELT on Google Cloud: load first into **BigQuery**, then transform with **SQL scripting / scheduled queries / UDFs / stored procedures / remote functions / notebooks**, and scale workflows with **Dataform**.
* **How to use:** Read top-down. Skim the **Exam Tips**. Copy the examples to practise.

---

## 1) 🧭 What is ELT? (mental model + baseline architecture)

**ELT = Extract → Load → Transform** (transform happens **after** loading into BigQuery).

**Why ELT on GCP**

* ✅ **Simplicity & speed**: land data quickly in BigQuery (**staging**), transform later.
* ✅ **Scale**: push heavy transforms to BigQuery’s engine.
* ✅ **Options**: SQL scripts, scheduled queries, Python/Notebooks, **Dataform**.

**Baseline ELT flow**

1. **Extract + Load** into **BigQuery staging tables**.
2. **Transform in BigQuery** (SQL scripting / scheduled queries / functions / tools like Dataform).
3. **Publish** to **production tables/views** for BI/ML.

> 💡 **Exam Tip**
> If the prompt says “*load first, transform in BigQuery*” or “*use BigQuery compute for transforms*”, the answer is **ELT in BigQuery** (often with **Dataform** when workflows get complex).

---

## 2) 🔁 A common ELT pipeline on Google Cloud

* **Ingestion**: EL/replication tools (e.g., `bq load`, BDTS, Datastream→BQ/GCS) write to **staging**.
* **Transform**:

  * **BigQuery SQL scripting** (procedural SQL: multi-step logic).
  * **Scheduled queries** (recurring transforms).
  * **UDFs** and **stored procedures** (reusable logic).
  * **Remote functions** (call Python via Cloud Run from SQL).
  * **Notebooks + BigQuery DataFrames** (Python exploration/transforms).
  * **Dataform** (SQL workflow management: dependencies + tests + automation).
* **Publish**: curated **prod** tables/views (with governance).

---

## 3) 🧾 BigQuery transformation toolkit (scripts, functions, scheduling)

### 3.1 SQL Scripting (procedural SQL)

BigQuery supports a **procedural language** so you can run **multiple SQL statements in sequence** with **shared state**.

* Control flow: `IF`, `WHILE`
* Multi-statement blocks: `BEGIN … END`
* **Transactions** for integrity
* **Variables** (including system variables)

> 💡 **Exam Tip**
> “Multiple SQL statements + shared state + IF/WHILE + transactions” → **BigQuery SQL scripting**.

---

### 3.2 UDFs (user-defined functions)

* **SQL UDFs** (recommended when possible) or **JavaScript UDFs**
* Scope: **temporary** or **persistent**
* JavaScript UDFs can use **external libraries**; community UDFs exist

Use when: you need **reusable transformation logic** across many queries.

---

### 3.3 Stored Procedures

* Encapsulate complex logic as a reusable unit
* Benefits: **reusability**, **parameterisation**, **transaction handling**, maintainability
* Called from apps or within SQL scripts

**Spark stored procedures on BigQuery**

* Can be defined in the BigQuery PySpark editor or via `CREATE PROCEDURE`
* Languages: **Python / Java / Scala**
* Code can be inline or stored in **Cloud Storage**

---

### 3.4 Remote Functions (Cloud Run)

Remote functions let BigQuery call code running in **Cloud Run**.

* Define the remote function in BigQuery (connection + endpoint)
* Use it in SQL like a UDF
* Useful for complex transformations in **Python**

> 💡 **Exam Tip**
> “Call Python logic from inside BigQuery SQL” → **Remote function + Cloud Run**.

---

### 3.5 Notebooks & BigQuery DataFrames

* Python exploration/transforms over datasets larger than RAM
* Integrates with visualisation libraries
* Can schedule notebook executions (useful for repeatable analysis pipelines)

---

### 3.6 Saved & Scheduled Queries

* Save queries, manage versions, share
* Schedule frequency + start/end times + destination settings

**Limit:** scheduled queries are great for simple jobs, but real pipelines often need post-steps:

* run another SQL script
* run data quality checks
* apply security steps

> 💡 **Exam Tip**
> “Need multiple dependent steps + tests + automation” → **Dataform** (not only scheduled queries).

---

## 4) 🧰 Dataform (SQL workflow orchestration for BigQuery ELT)

**What it is (clear + high-yield):**
Dataform is a **managed (serverless) tool to organise and run SQL transformations in BigQuery**. You use it when your **data is in BigQuery** (or at least queryable from BigQuery), and you want more than “just running one query”.

It helps you manage, in one place:

* **Transformations** (build tables/views from other tables)
* **Assertions** (SQL-based data quality checks, e.g., “no nulls”, “no duplicates”)
* **Automation** (run the workflow manually or on a schedule)

**How it works (plain):**

1. You write your logic as **SQLX** (SQL + config) and optionally **JavaScript** for reusable patterns.
2. Dataform builds a **dependency graph** (which tables must run first), validates/compiles your code, and shows errors early.
3. When you run it (or schedule it), Dataform triggers **BigQuery jobs** to execute the compiled SQL in the correct order.

✅ **Key point:** Dataform does **not** replace BigQuery or Spark. It **doesn’t have its own compute**.
It’s an **orchestrator for BigQuery SQL**: **Dataform plans/runs the workflow; BigQuery does the actual processing.**

---

### 4.1 Repository & workspace structure

* `definitions/` → `.sqlx` definitions (tables/views/incrementals/declarations)
* `includes/` → JavaScript helpers
* Common files: `.gitignore`, `package.json`, `package-lock.json`, `workflow_settings.yaml`, optional `README.md`

---

### 4.2 SQLX file structure

```text
config { ... }          # metadata + materialisation + (optionally) tests
js { ... }              # reusable JS helpers (optional)
pre_operations { ... }  # SQL before main body (optional)
-- main SQL body --
post_operations { ... } # SQL after main body (optional)
```

---

### 4.3 Materialisation types (must know)

* `declaration` → reference an existing BigQuery table
* `table` → create/replace a table from a SELECT
* `incremental` → create then update with new data
* `view` → create/replace a view (optionally materialised)

---

### 4.4 Data quality + custom steps

* **Assertions** (SQL/JS) → data quality tests
* **Operations** → custom SQL before/after/during execution

> 💡 **Exam Tip**
> “Primary purpose of assertions?” → **data quality tests**.

---

### 4.5 Dependencies (execution order)

* **Implicit**: `${ref("node")}` creates a dependency automatically
* **Explicit**: `config { dependencies: [...] }`
* `resolve()` references without creating a dependency

Workflows are best visualised as a **DAG** (graph of dependencies).

---

### 4.6 Triggers / scheduling

* **Internal triggers**: manual run in Dataform UI or Dataform schedules
* **External triggers**: **Cloud Scheduler** or **Cloud Composer**

Execution still happens **inside BigQuery**.

---

## 5) 🧪 Lab: Create & execute a SQL workflow in Dataform (recap)

### Task 1 — Create repository

* BigQuery → Dataform → **CREATE REPOSITORY**
* **ID:** `quickstart-repository` · **Region:** `REGION`
* Copy the **Dataform service account**.

### Task 2 — Create & init workspace

* Repo → **CREATE DEVELOPMENT WORKSPACE**
* **ID:** `quickstart-workspace` → **INITIALIZE WORKSPACE**

### Task 3 — Define a view (`definitions/quickstart-source.sqlx`)

```sql
config { type: "view" }

SELECT "apples" AS fruit, 2 AS count
UNION ALL SELECT "oranges", 5
UNION ALL SELECT "pears", 1
UNION ALL SELECT "bananas", 0
```

### Task 4 — Define a table (`definitions/quickstart-table.sqlx`)

```sql
config { type: "table" }

SELECT fruit, SUM(count) AS count
FROM ${ref("quickstart-source")}
GROUP BY 1
```

### Task 5 — Grant IAM to Dataform SA

* **BigQuery Job User**
* **BigQuery Data Editor**
* **BigQuery Data Viewer**

### Task 6 — Execute workflow

* Workspace → **START EXECUTION** → Execute actions → **START EXECUTION**
* Outputs go to dataset **`dataform`**
* Check **Executions** for logs/status

---

## 6) 🧠 Decision cheats (tool picker)

* **Few steps / simple recurrence** → **Saved + Scheduled Query**
* **Complex SQL workflow** (dependencies + tests + operations + incrementals) → **Dataform**
* **Reusable transformation logic** → **UDF**
* **Reusable multi-statement routine with transactions** → **Stored Procedure**
* **Need Python logic callable from SQL** → **Remote function (Cloud Run)**
* **Python exploration / large-scale transformations** → **Notebooks + BigQuery DataFrames**

---

## 7) ✅ Micro-Checklist for the exam

* ELT = load to **BQ staging**, transform **in BigQuery**, publish to **prod**
* BigQuery scripting: multi-statement + shared state + IF/WHILE + transactions + variables
* UDFs: SQL preferred; JS for external libs; temp vs persistent
* Stored procedures: reusable + parameterised + transaction handling; Spark procedures exist (Py/Java/Scala; inline or GCS)
* Remote functions: BigQuery calls Cloud Run from SQL
* Scheduled queries: automate cadence + destination
* Dataform: SQLX structure, materialisations, assertions, operations, dependencies (`ref` / `dependencies` / `resolve`), internal/external triggers; runs in BigQuery

---

### 👩‍🏫 Teacher’s nudge

If you can (1) define ELT precisely, (2) name BigQuery’s main transformation mechanisms, and (3) explain why Dataform is used for **workflow complexity + data quality + automation**, you’ll cover most exam questions in this module.

---

If you want, I can also produce a **short “exam-only” version** of this README (1 page) while keeping the same wording and decision rules.
