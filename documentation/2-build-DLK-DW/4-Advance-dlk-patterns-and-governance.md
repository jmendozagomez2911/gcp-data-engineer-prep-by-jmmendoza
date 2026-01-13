# 🏛️🧠 **README — Module 04: Advanced Lakehouse Patterns & Data Governance**

**Goal:** Build a *governed* lakehouse on Google Cloud: **catalog + security + ML on the data + real-world architecture + migration strategy**.

**How to use:** Read top-down. Skim **Exam Tips**. Copy the SQL examples to practise.

---

## 1) 🧭 Why this module exists (the lakehouse reality check)

A modern data platform must do **all** of this at once:

* **Governance & discoverability** (what data exists, who owns it, what it means)
* **Security & privacy** (PII protection, least privilege, fine-grained access)
* **Advanced analytics & ML** (without moving data everywhere)
* **Scalability & migration** (get there safely, step-by-step)

Google Cloud’s lakehouse pattern typically means:

* Data sits in **Cloud Storage** (lake) and **BigQuery** (warehouse),
* Access can be unified via **BigLake**,
* Governance is centralised with **Dataplex**,
* Sensitive fields are discovered/protected with **Sensitive Data Protection**,
* ML can run **in BigQuery (BigQuery ML)** or in **Vertex AI** (advanced, custom, MLOps).

> 💡 **Exam Tip**
> If the question says “**governed lakehouse**” or “**catalog + security across BQ/GCS/BigLake**” → think **Dataplex + IAM + fine-grained policies**, with **Sensitive Data Protection** for PII.

---

## 2) 🗂️ Dataplex — the metadata hub (universal catalog)

### 2.1 Why metadata matters

**Metadata = data about data**, e.g.:

* who created/owns it
* when it was created/updated
* what it contains (schema, business meaning)
* lineage/relationships
* sensitivity classification

In practice: metadata is what lets you **find the right table** and **trust it**.

### 2.2 What Dataplex does (for the exam)

Dataplex acts as a **universal catalog / governance layer** across:

* **BigQuery datasets/tables**
* **Cloud Storage data lakes**
* **BigLake tables** (lakehouse access)

Core idea: you want one place that answers:

> “What data assets exist, who owns them, how sensitive are they, and how should they be used?”

> 💡 **Exam Tip**
> If asked “primary function of Dataplex?” → **universal catalog for data assets across BigQuery, Cloud Storage, and BigLake**.

---

## 3) 🕵️‍♀️ Sensitive Data Protection (SDP) — find & protect PII

Cymbal’s key risk: **PII exposure** (emails, phone numbers, free text comments, addresses, credit cards, IDs).

### 3.1 The three core functions (memorise)

**Sensitive Data Protection = Discovery + Classification + Protection**

1. **Discovery**: scan sources (often BigQuery in lakehouse) and identify sensitive fields
2. **Classification**: label what type of sensitive data it is (email, phone, address, card, etc.)
3. **Protection**: apply de-identification or masking transformations (depending on needs)

> 💡 **Exam Tip**
> “Three core functions” question → **Discovery, Classification, Protection**.

### 3.2 The real demo vibe (what to remember)

* You can run **discovery jobs** over a project/dataset/table.
* You can choose inspection templates (what patterns to look for).
* You can output findings back into **BigQuery tables** for review (samples, inspection, risk output).

### 3.3 Tokenisation vs masking vs redaction (quiz-grade)

When you must “hide PII but still count uniques / keep joins possible”:

✅ **Tokenisation with a cryptographic hash**
*non-reversible*, but consistent → preserves “same email = same token”.

Other transformations:

* **Masking**: show partial (e.g., `jo***@domain.com`) — reversible in the sense it leaks structure
* **Redaction**: replace with placeholder (`[SENSITIVE]`)
* **Anonymisation/removal**: drop column entirely

> 💡 **Exam Tip**
> “Replace email with non-reversible token but still count unique customers” → **Tokenisation (cryptographic hash)**.

---

## 4) 🔐 IAM + fine-grained security (how access is *actually* controlled)

### 4.1 IAM: the foundation (least privilege)

Typical lakehouse access layers:

* **Cloud Storage (GCS)**: bucket/object permissions (often restricted to ingestion engineers + service accounts)
* **BigQuery**: dataset/table permissions (analysts read curated, data scientists write sandbox)
* **BigLake**: extends **BigQuery-style fine-grained controls** to data in object storage

### 4.2 Fine-grained security: row vs column (memorise)

**Column-level security**

* Restrict specific columns (perfect for PII like contact info)
* Example: marketing can see purchase history, **not** email/phone/address

**Row-level security**

* Restrict which rows a user can access
* Example: regional manager sees only their region’s rows

**Dynamic data masking** (especially relevant to lakehouse governance)

* Users can query, but sensitive values are masked depending on policy/role
* Mentioned as applicable for **BigLake tables** in Cloud Storage

> 💡 **Exam Tip**
> “Analyst must not see contact info (PII)” → **Column-level security** (most effective).

---

## 5) 🧠 ML directly on the lakehouse

### 5.1 BigQuery ML (BQML) — ML for analysts with SQL

BigQuery ML lets you **train + evaluate + predict** using SQL, without exporting data to another platform.

**Churn example (binary classification)**

#### Step 1 — feature engineering (signals)

Common churn features:

* `recency` (days since last purchase)
* `frequency` (purchases in last year)
* `monetary_value` (total spent)
* `tenure` (days since first purchase)

#### Step 2 — model training (key statement!)

```sql
CREATE OR REPLACE MODEL cymbal_ecommerce.customer_churn_predictor
OPTIONS(model_type='LOGISTIC_REG') AS
SELECT
  customer_id,
  recency,
  frequency,
  monetary_value,
  (total_purchases > 1) AS will_return
FROM cymbal_ecommerce.customer_purchase_summary;
```

#### Step 3 — evaluation

Use **`ML.EVALUATE`** (accuracy, precision, recall…)

#### Step 4 — prediction

Use **`ML.PREDICT`** (probability of churn)

> 💡 **Exam Tip**
> The SQL statement that *begins training* in BigQuery ML is **CREATE MODEL**.

### 5.2 Vertex AI — advanced ML + MLOps

Use Vertex AI when you need:

* custom/recommendation systems
* complex training pipelines
* model registry + deployment
* monitoring for drift
* automated retraining and CI/CD-style ML operations (**MLOps**)

> 💡 **Exam Tip**
> “Complex product recommendation engine + end-to-end ML platform” → **Vertex AI**.

---

## 6) 🥇🥈🥉 Real-world lakehouse architecture: Medallion pattern

**Medallion architecture** = three zones with increasing refinement:

### Bronze (raw / landing)

* Raw ingested data (streaming + batch)
* Often immutable
* Examples:

    * clickstream landing in GCS (Pub/Sub → GCS)
    * CSV/Avro exports from transactional DB
    * JSON campaign data

### Silver (cleansed + conformed)

* Cleaned and standardised
* Deduped, validated schemas, consistent keys
* “Ready for general internal use”

### Gold (curated business-level)

* Aggregated, highly refined, BI-ready
* “Executive dashboards / KPI tables / semantic layer-ready”

> 💡 **Exam Tip**
> “Final, highly refined, aggregated data optimised for reporting” → **Gold zone**.

---

## 7) 🚚 Migration strategies to a modern lakehouse (how it’s done in real life)

Big migrations fail when they are “big bang”. The winning approach is **phased, use-case-driven**.

### Step 1 — establish the foundation (always first)

* Set up project, networking, IAM model
* Create GCS buckets for **Bronze/Silver/Gold**
* Set up **Dataplex** for metadata/governance across the lakehouse

### Step 2+ — migrate by use case (incremental wins)

* Move one domain/pipeline at a time
* Keep old + new running in parallel until validated
* Expand governance + security policies as adoption grows

### Cost management essentials (exam-friendly)

* Use appropriate **GCS storage classes** (Nearline/Coldline for infrequent raw)
* Optimise BigQuery scanning with **partitioning + clustering**
* Use query cost estimation and train analysts on efficient SQL
* Consider **flat-rate capacity** for predictable workloads
* Set budgets and alerts in billing

---

## 8) ✅ Micro-Checklist for the exam

* **Dataplex** = universal metadata hub / catalog across **BQ + GCS + BigLake**
* **Sensitive Data Protection** core functions = **Discovery + Classification + Protection**
* Transformation to keep unique counts but hide emails = **tokenisation with cryptographic hash**
* Fine-grained controls:

    * **Column-level** for PII fields
    * **Row-level** for regional/data-slice restrictions
    * **Dynamic masking** especially for governed external/lakehouse access
* BigQuery ML:

    * training starts with **CREATE MODEL**
    * evaluate with **ML.EVALUATE**
    * predict with **ML.PREDICT**
* Vertex AI for complex custom ML + MLOps
* Medallion architecture zones:

    * **Bronze** raw, **Silver** cleansed, **Gold** curated/aggregated

---

## 9) 🎯 Practice prompts (exam-style)

1. “We need a single catalog of data assets across BigQuery and Cloud Storage, with governance.” → **Dataplex**
2. “Scan BigQuery tables to find email/phone/addresses automatically.” → **Sensitive Data Protection (Discovery/Classification)**
3. “Replace emails with a non-reversible value but still count unique customers.” → **Tokenisation (cryptographic hash)**
4. “Analyst can query customers but must not see contact columns.” → **Column-level security**
5. “Train churn model directly in BigQuery using SQL.” → **BigQuery ML (CREATE MODEL)**
6. “Build a custom recommendation engine with retraining + drift monitoring.” → **Vertex AI**
7. “Where do KPI-ready aggregated tables live in medallion?” → **Gold**

---

### 👩‍🏫 Teacher’s nudge

If you can (1) say what **Dataplex** is in one sentence, (2) explain **SDP = discover/classify/protect**, (3) distinguish **BigQuery ML vs Vertex AI**, and (4) map **Bronze/Silver/Gold**, you’re answering exactly how the exam expects.
