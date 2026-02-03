## Module — Developing Pipelines on Dataflow: Beam concepts + ETL lab + Templates

This “second installment” is basically saying: **you already know what Dataflow is**, now you learn **how to build real pipelines** with the Beam SDK (and later, Dataflow SQL).

### 1) The 3 ways to launch a Dataflow job (review)

1. **Templates in Cloud Console (Create Job Wizard)**
   No code. Pick a template + fill parameters → job runs.

2. **Write Beam code (Java/Python/Go) and run it**
   Java in IDEs (e.g., IntelliJ), Python often via notebooks or scripts. This is the **most powerful** option.

3. **Dataflow SQL UI**
   Write SQL (including streaming extensions) → Dataflow job runs.

This course mainly goes deeper on **#2 Beam SDK**, and later covers **#3 Dataflow SQL**.

---

## 2) Beam’s “4 pillars” (the mental model you must nail)

From Israel’s section:

### A) **Pipeline**

A **Pipeline** is the *graph* of what you want to do: read → transforms → write.

### B) **PCollection**

A **PCollection** is your distributed dataset (think “distributed list/table of elements”).
Key property: **immutable**.

**Meaning:** a transform never edits a PCollection in place.
It always does: `PCollection_in -> PTransform -> PCollection_out`.

**Why immutability matters (in distributed systems):**

* No shared mutable state ⇒ less coordination/locking
* Easier retries and fault tolerance
* Easier parallel execution

### C) **PTransform**

A **PTransform** is one step in the pipeline graph (map/filter/group/join/write, etc.).

### D) **Runner**

The **Runner** executes the pipeline on some environment:

* local machine (DirectRunner)
* cloud service (Dataflow)
* other engines (Flink, Spark runner, etc.)

> Interview-friendly phrasing: “Beam is the portable programming model; the runner is the execution engine.”

Speakers: Mehran Nazir, Israel Herraiz, Omar Ismail. Platform: Google Cloud.

---

## 3) The most important transforms (and the real meaning)

### `ParDo` (the “do anything per element” workhorse)

* Applies your function to **each element**.
* Like map/filter/flatMap, but more powerful.
* In practice, you can often use **convenience transforms** (Map/Filter/FlatMap) when you don’t need full DoFn lifecycle hooks.

### `GroupByKey` vs `CoGroupByKey` vs `Combine`

* **GroupByKey**: gathers all values for the same key together.
* **CoGroupByKey**: joins *multiple keyed PCollections* on the key. (This is the real “join primitive”.)
* **Combine**: does aggregation more efficiently when the operation is **associative + commutative** (e.g., sum, count). It can do partial combines in a tree-like way.

**Hot key / skew trigger (very exammy):**

* If one key has *huge* volume → `GroupByKey` can blow up a worker (“hot key”).
* If you’re aggregating with associative/commutative ops → prefer **Combine** over naive GroupByKey-based aggregation because it scales better.

### `Flatten` (NOT a join)

* Merges multiple PCollections of the **same type** into one.
* No key-based matching. No pairing.

### `Partition`

* Splits one PCollection into multiple PCollections based on a function (opposite vibe of Flatten).

---

## 4) Bundles + DoFn lifecycle (this is what people usually “half understand”)

Beam doesn’t process “the whole PCollection at once”. The runner splits it into **bundles**.

### What is a bundle?

A **bundle** is a chunk of elements processed together on a worker.

* The runner chooses bundle size.
* Streaming often prefers smaller bundles (lower latency, easier checkpointing).
* Batch can prefer larger bundles (throughput).

### DoFn lifecycle hooks (what they are for)

If you write a DoFn (especially in Java), you can use:

* `@Setup` — **once per DoFn instance** (often effectively “once per worker process”). Use it for expensive initialisation (DB client, connection pools, etc.).
* `@StartBundle` — when a new bundle starts (reset counters, metrics, buffers).
* `@ProcessElement` — called for each element in that bundle (your main logic).
* `@FinishBundle` — after the last element of the bundle (flush buffered writes, batch calls).
* `@Teardown` — cleanup (close connections).

### The warning in the transcript (you should remember)

* Don’t rely on mutable class members as “state”. The runner can retry, re-run, or move work.
* For “real state”, use Beam **State and Timers** (covered later), not random mutable variables.

---

# Lab — Writing an ETL pipeline (Java / Python): what it’s really teaching you

This lab is intentionally basic ETL, but it sneaks in the core production workflow:

## Part 1: Build ETL from scratch

### Step 1: Generate synthetic logs → put in GCS

You get `events.json` in a bucket.

### Step 2: **Read** (root transform)

* Java: `TextIO.read().from("gs://.../events.json")`
* Python: `ReadFromText("gs://.../events.json")`

This creates your first **PCollection[String]**.

### Step 3: Validate locally first (**DirectRunner**)

You run cheap and fast locally before paying for cloud execution.

### Step 4: Transform (parse JSON)

* Java: `ParDo` with Gson → output a typed object (e.g., `CommonLog`)
* Python: `Map(json.loads)` → output a dict

### Step 5: Write to BigQuery

* Java: `BigQueryIO.write().useBeamSchema()` (because your `CommonLog` is annotated with Beam schema)
* Python: `WriteToBigQuery(..., schema=...)` (string or JSON schema)

### Step 6: Run on Dataflow (**DataflowRunner**)

You set:

* `--stagingLocation` (where code/artifacts are staged)
* `--tempLocation` (temp files, intermediate stuff)
* `--region`
* `--runner=DataflowRunner`

And you observe workers appear/disappear (autoscaling).

---

## Part 2: Parameterisation + Templates (why they matter)

The big idea is: **separate “build” from “run”.**

### Without templates

Only developers with an environment can run it easily.

### With templates

* Devs build once, stage a template spec to GCS.
* Non-devs (or Airflow/CLI/API) launch jobs by passing parameters.

That’s why the template version needs:

* **schema.json** (runtime-provided schema)
* a JS UDF (to transform text into JSON)
* ValueProviders / nested providers (because values are unknown at compile time)

---

# Quiz 1 — answers + reasoning

### 1) How many times is `processElement` called?

✅ **As many times as there are elements in the PCollection.**

* Conceptually, it’s “per element”.
* In real systems, retries can happen, but the quiz wants the model answer: **one call per element**.

### 2) What is `CoGroupByKey` used for?

✅ **To join data in different PCollections that share a common key.**

* It’s the join primitive across collections keyed the same way.
* The option mentioning “common value type” is wrong; values can differ.

### 3) What happens when a PTransform receives a PCollection?

✅ **It creates a new PCollection as output, and it does not change the incoming PCollection.**

* That’s Beam immutability: transforms don’t mutate inputs.