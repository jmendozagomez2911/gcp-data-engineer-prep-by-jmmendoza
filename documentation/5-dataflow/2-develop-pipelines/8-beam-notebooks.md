# 🧠 Module — Beam Notebooks & Interactive Development on Dataflow

This module explains **why normal Beam development can feel slow during exploration**, and how **Beam notebooks + the Interactive Runner** improve that experience.

The core idea is:

> **Production Beam execution is optimised for running pipelines efficiently, but not for inspecting them interactively while you are still building them.**

Beam notebooks solve that gap.

---

## 1) Why this part exists

When you develop a normal Beam pipeline, the flow is usually:

1. write the pipeline
2. submit the job
3. wait for execution
4. inspect logs / print statements
5. change code
6. resubmit

That is acceptable for **production-oriented execution**, but painful for **early development**, especially when you are still trying to understand:

* the raw input data,
* what each transform is doing,
* and the intermediate results between transforms.

So this module exists to teach a different mindset:

* **Dataflow service** is great for scalable execution
* **Interactive Runner + notebooks** are better for exploration and debugging during development

---

## 2) The production model vs the development model

### Normal Beam / Dataflow execution

In standard Beam development, you **declaratively describe** the pipeline and submit it to the runner.

That is great for production because the runner can do optimisations such as:

* stage fusion
* efficient execution planning
* overall better performance at runtime

So the standard model is ideal when the pipeline logic is already stable.

### The problem during development

While building the pipeline, this model is awkward because you often want to inspect:

* the input as it arrives,
* intermediate `PCollections`,
* and partial results after transforms

With standard job submission, that usually means adding logs or sinks just to see what is happening.

That is exactly the friction this module is trying to remove.

---

## 3) What the Interactive Runner gives you

The **Interactive Runner** lets you work with Beam pipelines more like an exploratory environment.

Its main value is:

> **access to intermediate results from transformations**

That means you do not always need to:

* add extra logging,
* add temporary sinks,
* or repeatedly submit full jobs just to inspect data.

### Important point

The transcript explicitly says the Interactive Runner works with:

* **batch sources**
* **streaming / unbounded sources**

That is important because it means you do **not** always need to mock streaming inputs during development.
You can work directly against real data sources when needed.

---

## 4) Why notebooks are useful here

Beam notebooks provide the environment where this interactive workflow becomes practical.

The module describes Dataflow notebooks as a hosted notebook environment where:

* you create a notebook instance,
* the needed libraries are already installed,
* and you can start developing immediately.

So the notebook is not the main feature by itself.
Its real value is that it gives you a ready-made place to use the **Interactive Runner** comfortably.

### Extra benefit

The environment also comes with **example notebooks**, which are useful for:

* learning,
* experimentation,
* and quick starting points

The transcript uses a **word count** example to demonstrate the workflow.

---

## 5) Example development flow in the notebook

The example pipeline shown is roughly:

1. **Read from Pub/Sub**
2. Apply a **fixed window of 10 seconds**
3. **Count** elements

Normally, if you were not using the Interactive Runner, you would then need another step such as:

* logging,
* or writing to a sink,

just to inspect the result.

With the Interactive Runner, you can inspect intermediate collections directly.

That is the key shift:

> **the pipeline becomes inspectable while you are still building it**

---

## 6) The special problem with unbounded sources

When you develop against a streaming source, there is an obvious problem:

> **the source does not end by itself**

So the notebook needs a way to know how much data to capture.

The transcript mentions two important Interactive Runner options:

### `recording_duration`

Limits recording to a **fixed amount of time**.

Use this when you want:

* “capture 30 seconds of stream”
* “read for 1 minute and stop”

### `recording_size_limit`

Limits recording to a **fixed number of bytes**.

Use this when:

* the stream is high volume,
* you want to protect the notebook from too much data,
* or you only need a controlled sample.

### Practical decision rule

* If the requirement is **time-based sampling of a stream** → use **recording duration**
* If the concern is **volume / notebook overload** → use **recording size limit**

This second one is especially practical in real systems where a live stream may be too large to inspect safely inside a notebook.

---

## 7) Reusing data vs fetching fresh data

Another useful feature mentioned is the ability to:

* **reuse the recorded stream data**
* or **fetch fresh data**

### Why reuse matters

Reuse is helpful when you are exploring transformations and want to keep working on the **same captured data**.

That gives you repeatability during debugging.

### Why fresh data matters

Fresh data is useful when the current state of the stream matters, or when you want to validate behaviour on new events.

So the trade-off is:

* **reuse** → stable exploration
* **fresh** → current reality

---

## 8) How you inspect the data

The transcript mentions a few notebook-specific ways to inspect results.

### `ib.show(...)`

This lets you **visually inspect** the data.

It can also include extra metadata such as:

* event time
* the window an element belongs to

That is especially useful for streaming pipelines because it helps you understand not just the value, but also its **time context**.

### `ib.collect(...)`

This lets you collect results into a **Pandas DataFrame**.

That is useful when you want to:

* manipulate the results directly in notebook code,
* analyse them further,
* or create custom visualisations.

### Visual exploration

The notebook also supports visual exploration through the UI when visualisation is enabled.

The transcript mentions setting visualisation to true so you can inspect data more graphically.

So there are really two inspection modes:

* **show it visually**
* **collect it into Pandas and manipulate it programmatically**

---

## 9) The main benefit: faster iteration

This is the practical value of the whole module.

Beam notebooks let you get away from the painful cycle of:

> write → submit → wait → inspect logs → change → resubmit

and move towards:

> write → inspect intermediate data → adjust → continue

That is a much better workflow for:

* learning,
* debugging,
* data exploration,
* and early pipeline design.

---

## 10) Moving from development to production

A very important point in the transcript is that the transition to production is small.

Why?

Because throughout the notebook workflow, you are still writing **Beam SDK code**.

So the notebook is not a separate programming model.
It is still Beam.

To submit the pipeline to the Dataflow service, the module says you mainly need to:

* import the **Dataflow runner**
* provide pipeline options such as:

    * project
    * staging directory
    * other required runner options
* then run the pipeline on the service

### Why this matters

The notebook workflow is not throwaway work.

It is meant to reduce development friction **without forcing you to rewrite everything later**.

That is one of the most important takeaways from this module.

---

## ✅ Exam takeaways

* **Interactive Runner** is for **development-time exploration**, not the main production execution model
* It gives access to **intermediate results**
* It works with **both batch and streaming sources**
* For unbounded sources, you need limits such as:

    * **recording duration**
    * **recording size limit**
* You can **reuse captured stream data** or fetch **fresh data**
* `ib.show` helps inspect results visually, including metadata like **event time** and **window**
* `ib.collect` lets you bring results into a **Pandas DataFrame**
* Moving from notebook development to Dataflow production requires **little code change** because you are still using Beam SDK code underneath

---

## ⚠️ Common trap

Do **not** think Beam notebooks are a separate execution system.

They are just a more interactive development environment on top of the same Beam model.

So the correct mental model is:

> **Use notebooks + Interactive Runner to explore and debug.
> Use Dataflow runner to execute at production scale.**
