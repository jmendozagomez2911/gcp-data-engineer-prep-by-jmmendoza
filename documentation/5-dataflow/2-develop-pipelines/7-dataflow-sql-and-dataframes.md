# 🧠 Module — Beam SQL & Beam DataFrames

This module introduces **two higher-level ways to express Beam pipeline logic**:

1. **Beam SQL / Dataflow SQL**
2. **Beam DataFrames**

The key point is simple:

> They do **not** replace Beam. They are just **more concise ways** to express logic that Beam will still execute underneath.

---

## 1) Why this matters

Raw Beam code can become verbose for operations like:

* joins
* filters
* projections
* aggregations

That is why SQL is useful here: it expresses relational logic more directly and with less boilerplate.

The same idea applies to DataFrames in Python: instead of writing lower-level Beam transforms, you can use a more familiar **Pandas-like API**.

---

## 2) Beam SQL vs Dataflow SQL

### Beam SQL

Beam SQL lets you query **bounded and unbounded PCollections** using SQL.

Important points:

* embedded programmatically with **`SQLTransform`**
* can be mixed with normal **PTransforms**
* supports **UDFs**
* works with **schema-aware PCollections**
* supports **windowing for aggregations on unbounded data**

### Dataflow SQL

Dataflow SQL is basically the **UI / CLI-facing way** to use Beam SQL on Dataflow.

Important distinction:

* **Beam SQL** = programmatic interface
* **Dataflow SQL** = UI/CLI experience on top of Beam SQL

A practical workflow is:

* analyst tests logic on historical data in **BigQuery UI**
* analyst adapts similar SQL for streaming in **Dataflow SQL**
* engineer embeds that SQL into production pipelines with **`SQLTransform`**

That handoff is one of the main ideas in this module.


    public class BeamSqlExample {
    public static void main(String[] args) {
    Pipeline p = Pipeline.create();

        // 1) Definir el schema de las filas
        Schema productSchema = Schema.builder()
                .addInt32Field("id")
                .addStringField("name")
                .addDoubleField("price")
                .build();

        // 2) Crear datos de ejemplo
        Row row1 = Row.withSchema(productSchema).addValues(1, "Laptop", 1200.0).build();
        Row row2 = Row.withSchema(productSchema).addValues(2, "Mouse", 25.0).build();
        Row row3 = Row.withSchema(productSchema).addValues(3, "Monitor", 300.0).build();

        // 3) Crear la PCollection<Row>
        PCollection<Row> products = p.apply(
                Create.of(row1, row2, row3).withCoder(RowCoder.of(productSchema))
        );

        // 4) Aplicar Beam SQL
        PCollection<Row> expensiveProducts =
                products.apply(
                        SqlTransform.query(
                                "SELECT id, name, price " +
                                "FROM PCOLLECTION " +
                                "WHERE price > 100"
                        )
                );

        // 5) Mostrar el resultado
        expensiveProducts.apply(org.apache.beam.sdk.transforms.MapElements
                .into(org.apache.beam.sdk.values.TypeDescriptors.strings())
                .via(row -> row.toString()))
                .apply(org.apache.beam.sdk.io.TextIO.write().to("output/products").withoutSharding());

        p.run().waitUntilFinish();
    }}
---

## 3) SQL dialects

Beam SQL supports two dialects:

* **Calcite SQL**
* **ZetaSQL**

### Calcite SQL

* default Beam SQL dialect
* mature
* supports things like **Java UDFs**

### ZetaSQL

* more compatible with **BigQuery Standard SQL**
* useful when working with **BigQuery-oriented pipelines**

The transcript frames **Dataflow SQL** around a **ZetaSQL-like / BigQuery-like syntax**.

---

## 4) Windowing in SQL

This module also shows how Beam streaming windows appear in SQL.

* **TUMBLE** → fixed / tumbling windows
* **HOP** → sliding / hopping windows
* **SESSION** → session windows

Exam rule:

* fixed non-overlapping windows → **TUMBLE**
* overlapping windows → **HOP**
* inactivity-based grouping → **SESSION**

---

## 5) Beam DataFrames

Beam DataFrames provide a **Pandas-like API** in the Beam Python SDK.

Main idea:

> They look like DataFrames, but they still follow Beam’s distributed and deferred execution model.

That means they are **not ordinary Pandas DataFrames**.

### What changes compared with Pandas

Operations are **deferred** until pipeline execution.

So before execution, you typically know:

* schema
* column names
* column types

But not the actual result values.

### Important limitations

Because Beam collections are distributed and unordered, some Pandas operations are not supported, especially ones that depend on:

* row order
* materialised values
* interactive inspection

Examples mentioned:

* **transpose**
* **shift**
* order-sensitive operations like **head** and **tail**

---

## 6) Quiz 7

### Q1. What operations can you do in standard Pandas DataFrames that are not possible in Beam DataFrames?

**Correct answers:**

* **Write the DataFrame columns as rows**
* **Shift the DataFrame**

Why:

* “write columns as rows” refers to **transpose**
* **shift** depends on row ordering
* Beam DataFrames do not support those kinds of operations well because execution is deferred and collections are unordered

### Q2. Which two of the following interfaces support Calcite SQL?

**Correct answers:**

* **Beam SQL client**
* **Dataflow template**

Why:

* Beam SQL supports **Calcite SQL**
* Dataflow SQL is presented here as **ZetaSQL-oriented**
* a template can encapsulate Beam SQL logic programmatically

---

## ✅ Exam takeaways

* **Beam SQL** = SQL over Beam `PCollections`
* **Dataflow SQL** = Beam SQL exposed through UI / CLI on Dataflow
* **Calcite** = default Beam SQL dialect
* **ZetaSQL** = BigQuery-like dialect
* **TUMBLE / HOP / SESSION** = SQL forms of Beam windowing
* **Beam DataFrames** = Pandas-like API, but **deferred and limited by Beam semantics**
* operations like **transpose, shift, head, tail** are bad fits for Beam DataFrames
