## Module — Beam Schemas (and branching pipeline lab context)

### What problem schemas solve (first principles)

A **PCollection must be homogeneous**: all elements have the “same type” (e.g., JSON records, Avro records, Protobuf records, etc.).
But in a distributed runner (Dataflow), Beam must **encode/decode elements** (ship them across workers), so Beam needs a reliable way to understand **structure**.

**Schemas** give Beam a **type system for records**: fields + types + nullability + nesting — independent of any single language class.

---

## What a schema is (in Beam terms)

A schema describes structured records with:

* **Fields**: name → value
* **Primitive types**: int/long/string/double/…
* **Nullability**: REQUIRED vs NULLABLE
* **Nested records**: a field can itself have a schema
* **Arrays / maps**: repeated / keyed structures

This is basically “database-style thinking” applied to pipeline records.

---

## How a PCollection gets a schema attached

You only get the benefits if the **PCollection has a schema**.

Ways it happens:

1. **Source attaches it**

    * Example: **AvroIO** can infer Beam schema from the Avro schema and attach it automatically.
2. **You attach it via your types / coders**

    * Java: annotate a POJO with Beam schema annotations (e.g., `@DefaultSchema(JavaFieldSchema.class)`).
    * Protobuf: structure is inferable from the proto descriptor in many cases.
3. **Intermediate stages can also be schematized**

    * Even if the source doesn’t provide schema, you can convert into a schematized representation (often `Row`).

---

## Why schemas matter (what you get)

| Without schemas                                                       | With schemas                                                     |
| --------------------------------------------------------------------- | ---------------------------------------------------------------- |
| You manually map fields, cast types, create helper classes everywhere | You use **field-name-based transforms** that look like SQL logic |
| Joins/aggregations become verbose Java plumbing                       | Code focuses on **business logic**                               |
| Cross-language reasoning is harder                                    | Schema gives a shared structure across SDKs                      |

Key mental model: **Schemas turn your pipeline into “relational processing” on structured records.**

---

## “Row” is the universal schematized record

When you do schema transforms like **Select / DropFields / Filter(whereFieldName …)**, the output is often:

* `PCollection<Row>` (not your original POJO), because you changed the schema shape.

**Row = generic record that can hold any schema.**

---

## Code-level takeaways from the lab (Branching Pipelines + Schemas)

### 1) Branching pipeline (resource optimisation pattern)

You can apply multiple transforms to the *same* PCollection to create branches:

* **Archive everything** to GCS (cheap, durable — e.g., Coldline)
* **Send only analyst-needed subset** to BigQuery (reduce cost)

### 2) Filter by field (schema-based projection)

Instead of writing a DoFn to drop fields:

* Use **Select** / **DropFields** (schema transforms)
* Output becomes `Row`, so BigQuery write becomes `BigQueryIO.<Row>write()`

### 3) Filter by element (schema-aware filtering)

Use schema `Filter` (the newer one) like:

* `Filter.create().whereFieldName("num_bytes", (Long n) -> n < 120)`
  Important: this is **different** from the older non-schema `beam.transforms.Filter`.

### 4) Make pipelines parameterisable (production readiness)

* Custom `PipelineOptions` let you pass:

    * input path
    * output path
    * BigQuery table
* Args format: `--option=value`
* Register options interface so `--help` shows them.

### 5) NULLABLE fields (schema + BigQuery mode)

Java: mark fields nullable so Beam schema (and therefore BigQuery schema via `useBeamSchema()`) reflects that:

* `@javax.annotation.Nullable Double lat;`

---

# Quiz 4 — answers

### 1) Which element types can be encoded as a schema from a PCollection? (select ALL that apply.)

✅ **Protobuf objects**
✅ **Avro objects**

❌ Byte string objects (not structured; no field schema by default)
❌ A single list of JSON objects (JSON needs parsing + explicit structure; it’s not inherently a schematized element type as stated)

### 2) Is it possible to mix elements in Schema PCollections inside a single Beam pipeline? (Select the two correct answers.)

✅ **Yes, but only across different PCollections**
✅ **Not possible within the same PCollection**

Because: one **PCollection** must have elements of the **same type/schema**, but a **pipeline** can have many different PCollections with different schemas.
