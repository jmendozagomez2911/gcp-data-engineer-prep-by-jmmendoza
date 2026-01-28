# 🤖🧼 README — Module 03: Control Data Quality in Batch Data Pipelines

**Goal:** Build batch pipelines that **don’t break**, **don’t stop because of a few bad rows**, and produce **trusted analytics data** (BigQuery / lakehouse) using an exam-friendly pattern: **validate + cleanse + DLQ + logging + trend analysis** + **schema evolution**.

**Read me like this:** 1) Validation & cleansing → 2) DLQ (Dataflow vs Spark) → 3) Logging + error tables → 4) Schema evolution (additive vs breaking) → 5) Schema-on-write vs schema-on-read → 6) Iceberg schema evolution demo → 7) Exam cheats + quiz traps.

---

## 1) 🧠 Core idea: “Fast is useless if the data is wrong”

Cymbal Superstore’s batch billing data hits classic quality issues:

* Incorrect transaction amounts
* Missing customer IDs
* Duplicate entries (glitches)
* Schema differences across sources
* Address encoding inconsistencies

**Your engineering goal:** build logic that checks **completeness, conformity, consistency, and reasonableness**.

### Data validation vs data cleansing (don’t mix them up)

* **Validation** = *detect* rule violations (nulls, invalid types/formats, negative amounts, etc.).
* **Cleansing** = *fix* or remove issues (trim whitespace, standardise values, default values, etc.).

> **Exam tip:** Validation is about **rules**. Cleansing is about **correction/standardisation**. You almost always do both in batch pipelines.

---

## 2) ☠️ Dead Letter Queue (DLQ) — the standard batch pattern

A **DLQ** is how you handle bad data **without crashing the whole job**.

**Principle:**

* Valid records continue to the main pipeline.
* Invalid records are **routed** to a separate storage location for later review/fix.

> **Exam tip:** If the question says “process valid records without interruption while isolating invalid ones”, the answer is **DLQ**.

### DLQ implementation: Dataflow vs Serverless Spark (know the mapping)

| What you need                | Dataflow (Apache Beam)                                                      | Serverless for Apache Spark                                           |
| ---------------------------- | --------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| Where validation logic lives | **ParDo** with a **DoFn** (custom checks)                                   | DataFrame transformations (e.g., `withColumn`, `when`, `array_union`) |
| Routing mechanism            | **Multiple outputs**: main output + **tagged side output** (`TaggedOutput`) | Build an `errors` column, then **split with `filter()`**              |
| DLQ result                   | Side output PCollection = DLQ                                               | “Invalid rows” DataFrame = DLQ                                        |

> **Exam tip:** In Dataflow, “split outputs in one pass” screams **ParDo + tagged side output**.

---

## 3) 🧾 “Routing isn’t enough”: Logging + Error analytics

DLQ tells you **what** failed (the rows). Mature systems also need:

### A) Error logs = the **“WHY”**

* Row-level detail: **which rule failed**, what value was wrong, timestamp, identifiers.
* Used for **debugging a specific run**.

**Where it lands:**

* Dataflow templates log details to **Cloud Logging**
* Serverless Spark streams driver/executor logs to **Cloud Logging**

### B) Error tables = the **“HOW OFTEN”**

* Aggregated metrics, usually in **BigQuery**
* Lets you query trends:

    * “Which rule fails most?”
    * “Is invalid country code rising this month?”

> **Exam tip:** “Long-term trend analysis / systemic data quality issues” → **Error table in BigQuery** (not DLQ, not logs).

### Practical production pattern (what they want you to say)

1. **DLQ in Cloud Storage / Iceberg table**: preserve bad rows.
2. **Cloud Logging**: structured error messages for debugging.
3. **BigQuery error table**: aggregated counts by error type for dashboards/alerts.

---

## 4) 🧼 Validation & cleansing with Serverless for Apache Spark (the 5-step flow)

**Batch validation pipeline flow:**

1. **Input**: raw lands in GCS or Iceberg → read into a DataFrame
2. **Process**: apply rules + cleansing
3. **Split**: valid vs invalid using an `errors`/`validation_errors` column
4. **Output (curated)**: valid rows → trusted table
5. **Output (invalid)**: invalid rows → DLQ table for review/fix

### Key PySpark functions to recognise

* `when()` → conditional rule application
* `trim()` → cleansing whitespace / blank strings
* `concat()` → append error codes/messages to `validation_errors`
* `filter()` → split valid vs invalid

> **Exam tip:** Prefer **built-in Spark functions**. Avoid Python UDFs for validation (see quiz).

---

## 5) 🧱 Schema evolution in batch pipelines (structural changes)

Batch pipelines break when schema changes unless you design for it.

### Two categories you must classify

#### A) Additive (non-breaking)

* Add new nullable columns (most common)
* Usually can be handled automatically (tool features)

#### B) Breaking changes

* Delete columns
* Rename fields
* Change data type (STRING → INTEGER, etc.)
* Needs an architectural strategy to avoid downtime and corruption

> **Exam tip:** “Add column” → merge/evolve schema.
> “Rename/type change” → treat as breaking; use facade pattern.

### Batch constraint (important)

You **cannot update a running batch job**.
Handling schema evolution means: **deploy a new pipeline version** that succeeds on the next scheduled run.

---

## 6) How to handle schema evolution (tool-agnostic strategy)

### A) For additive changes (easy mode)

* **Dataflow templates / Job Builder UI**: enable schema evolution via parameters

    * Write disposition: append/overwrite
    * Schema update option: “allow field addition” (conceptually)
* **Serverless Spark templates**: pass something like `--merge-schema=true` so new columns land without failing

> **Exam tip:** If the scenario says “new field added, pipeline should not fail”, choose **schema merge / allow field addition**.

### B) For breaking changes (safe mode): **Facade View Pattern**

This is the “keep dashboards online” pattern.

1. Deploy a new pipeline writing to a **new table** (e.g., `data_v2`)
2. Create/update a **VIEW** that unions old + new (`UNION ALL`)
3. Point all consumers to the **stable view**, not the physical tables

**Why this matters:** Consumers stay stable while you migrate/transform behind the scenes.

> **Exam tip:** “Rename column, must keep downstream dashboards online” → **Facade View Pattern**.

---

## 7) Schema-on-write vs Schema-on-read (exam vocabulary)

### Schema-on-write (warehouse model, e.g., BigQuery)

* Schema enforced **at write time**
* Benefits: high query performance + strong data quality
* Trade-off: ingestion rigidity → you must manage schema changes carefully

### Schema-on-read (lake model, e.g., raw files in GCS)

* Store raw; apply schema **when reading**
* Benefits: flexible ingestion
* Trade-off: slower queries + governance risk (“data swamp”)

> **Exam tip:** BigQuery is the classic **schema-on-write** story in these courses.

---

## 8) Iceberg schema evolution demo (Serverless Spark)

### Business problem

Data science needs an `OS` column derived from the browser user-agent string.

### Technical approach (what you must recognise)

* Run a **Serverless Spark batch job**
* Execute **Spark SQL** against an **Apache Iceberg** table:

    1. `ALTER TABLE ... ADD COLUMN OS STRING`
    2. `UPDATE ... SET OS = CASE WHEN ... END WHERE OS IS NULL`

**Key point:** Iceberg supports **in-place**, **atomic** schema + data updates (transactional safety).

### Why they mention BigLake/BigQuery

After the job, you can query the updated Iceberg table via BigQuery (registered through BigLake) to validate the new column exists and is populated.

> **Exam tip:** “Add column + backfill in Iceberg” → Spark SQL `ALTER TABLE` + `UPDATE`, relying on Iceberg’s transactional guarantees.

---

## 9) ✅ Quiz answers + traps (straight from your transcript)

### Q1: Rename a column (breaking), dashboards must stay online

✅ **Facade View Pattern**

### Q2: Dataflow template “Cloud Storage Text to BigQuery” + DLQ path

✅ Catches **Parsing errors** + **Conversion errors**

### Q3: Dataflow split: valid → BigQuery, null IDs → GCS

✅ **ParDo with main output + tagged side output**

### Q4: Why avoid Python UDFs in Spark validation?

✅ **Performance**: Python UDFs are slow because Spark must move data between JVM-optimised execution and the Python interpreter row-by-row.

---

## 10) 🧠 Exam micro-checklist (memorise this vibe)

* DLQ = keep pipeline running; isolate bad rows
* Dataflow split in one pass = **ParDo + tagged side output**
* Dataflow templates DLQ auto-handles: **parsing + conversion**
* Logs = **WHY** (Cloud Logging)
* Error tables = **HOW OFTEN** (BigQuery trend analysis)
* Schema evolution:

    * Additive → allow field addition / merge schema
    * Breaking → **Facade View Pattern**
* Batch constraint: you can’t patch a running job; you deploy a new version for next run
* Avoid Python UDFs for validation in Spark (perf killer)