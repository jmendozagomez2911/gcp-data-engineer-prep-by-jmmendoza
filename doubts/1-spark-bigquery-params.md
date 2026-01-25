# Spark + BigQuery: `temporaryGcsBucket` vs `checkpointLocation` 🧭

## Goal

Avoid setting parameters “by inertia” and understand **when they are required** (and when they are not) when working with:

* CSVs in **GCS** (Google Cloud Storage)
* Reading/writing **BigQuery** from Spark
* **Batch** vs **Structured Streaming**

---

## 1) `checkpointLocation` (Spark Structured Streaming) 💾

### What it is

A path where **Spark Structured Streaming** stores:

* progress (offsets)
* query metadata
* state (if there are stateful operations)

### When you need it ✅

Only when you use **Streaming**, i.e.:

* `readStream` + `writeStream`

Especially if you have:

* windows / aggregations with watermark
* deduplication with watermark
* streaming joins
* *stateful* operations (e.g. `mapGroupsWithState`)

### When it is NOT used ❌

* **Batch** jobs (`read` + transform + `write` and the job ends)

> Quick rule: **if you don’t use `writeStream`, `checkpointLocation` is irrelevant.**

---

## 2) `temporaryGcsBucket` (Spark ↔ BigQuery connector) 🪣

### What it is

A GCS bucket that the **Spark-BigQuery connector** uses as **temporary staging** in “indirect” mode:

1. Spark writes temporary files to GCS
2. BigQuery runs a **load job** and loads those files into the table
3. Temp data is cleaned up (or left behind) depending on config/errors

### When you do NOT need it ❌

* If your job **does not write to BigQuery** (for example, you only read/write **CSVs in GCS**)

### When it IS mandatory ✅

When you write to BigQuery using **indirect / load jobs** (very common):

* `temporaryGcsBucket` must:

    * **exist**
    * be accessible by the identity (service account) running the job
    * have enough permissions (create/read/delete objects) 🔐
    * be in a compatible region (recommended: same region/multi-region as the BigQuery dataset) 🌍

> If you leave it as `""` and the connector tries *indirect*, it will typically fail.

---

## 3) “Direct write” (Storage Write API) vs “Indirect” ⚙️

### Indirect (via GCS + load jobs)

* **Requires** GCS staging → `temporaryGcsBucket` **mandatory**

### Direct (Storage Write API)

* In theory it **may not need** GCS staging
* In practice it depends on:

    * connector version
    * configuration (`writeMethod=direct`, etc.)
    * use-case compatibility (schema, partitioning, append/overwrite mode, etc.)

⚠️ Key risk: **fallback**

* Sometimes the connector can “fall back” to indirect due to limitations
* If that happens and you had `temporaryGcsBucket=""` → error

> Operational recommendation: even if you use direct, **keeping `temporaryGcsBucket` configured** often prevents surprises.

---

## 4) Final matrix (quick decision) ✅

### Case A: Batch reading/writing CSVs in GCS

* `checkpointLocation`: **NO**
* `temporaryGcsBucket`: **NO**

### Case B: Batch writing to BigQuery

* `checkpointLocation`: **NO**
* `temporaryGcsBucket`:

    * **YES** if using **indirect**
    * **MAYBE NO** if **direct is guaranteed** (no fallback), but it’s commonly configured anyway

### Case C: Structured Streaming writing to BigQuery

* `checkpointLocation`: **YES**
* `temporaryGcsBucket`:

    * depends on **direct vs indirect** (same rules as above)

---

## 5) Decision rules (ultra short) 🧩

1. **Do you use `writeStream`?**

    * Yes → `checkpointLocation` **yes**
    * No → `checkpointLocation` **no**

2. **Do you write to BigQuery?**

    * No → `temporaryGcsBucket` **no**
    * Yes →

        * Indirect → `temporaryGcsBucket` **yes**
        * Direct → may be no, but for robustness **better yes**

---

## Bucket naming doubt: can I reuse the same bucket name? 🪣✅

Yes—you **can** use the **same bucket** you already use for reading/writing CSVs.

Why it’s safe:

* The connector typically writes temporary data under **unique paths** (often UUID-based), so it won’t overwrite your normal files.
* The important thing is that the bucket is accessible and has the right permissions.

What’s recommended:

* **Best practice / convention**: use a **dedicated temp bucket** (cleaner operations, easier lifecycle rules, easier debugging, reduced risk of accidental cleanup of “real” data) 🧹
* **But it’s not required**: using your existing data bucket can work fine and integrates seamlessly.

---

## Anti-error checklist 🧯

* Bucket exists ✅
* Service account permissions ✅
* Region compatible ✅
* If you’re unsure about direct/indirect → set `temporaryGcsBucket` ✅
