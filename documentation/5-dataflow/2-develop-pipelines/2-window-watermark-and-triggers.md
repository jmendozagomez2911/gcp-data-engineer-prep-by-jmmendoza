## Module: Windowing, event time vs processing time, watermarks, triggers, accumulation (Dataflow / Apache Beam)

### 1) Why windowing exists in streaming

In **unbounded (streaming) data**, “the dataset never ends”, so an aggregation like **count / sum / group** needs a rule for **which elements belong together** and **when to emit results**.

A **window** is a logical “bucket” (usually time-based) that turns an unbounded stream into **finite chunks** so you can run bounded-style aggregations (e.g., `GroupByKey`, `Combine`, `Count`).

---

### 2) Two time dimensions

Beam/Dataflow reason about time in two main ways:

* **Processing time**: when the worker **sees** the element (arrival/execution time).

    * Simple, but sensitive to delays/out-of-order delivery.
* **Event time**: when the event **actually happened** at the source (timestamp carried by the data).

    * Best for “business truth” when events can arrive late or out of order.

**Key behaviour (event time windowing):**
If an event arrives late (e.g., phone offline, plane mode), Dataflow can still place it into the **correct event-time window** (as long as the window is still accepting late data).

---

### 3) Default window types (Beam)

* **Fixed windows**: non-overlapping blocks (e.g., every 1 minute / 1 hour).
* **Sliding windows**: windows overlap; defined by:

    * **window size** (e.g., 60s) and **period** (e.g., every 30s).
    * Typical use: **moving averages**.
* **Session windows**: per “burst of activity”, defined by a **gap duration** (if no events for X time, session closes). Data-dependent.

---

### 4) Watermarks: “how complete is the data?”

In the real world, events arrive out of order and late. So you need a concept of **progress** in event time.

A **watermark** is the runner’s estimate of “event time up to which we believe we’ve seen (almost) everything”.

* If an element arrives **after the watermark has advanced past that element’s event time**, it’s considered **late**.
* Dataflow estimates/updates the watermark continuously (it’s not perfectly knowable because it depends on data you haven’t seen yet).

**Important nuance (common exam trap):**

* “Late” is not about wall-clock time alone.
* “Late” is defined **relative to the watermark’s progress in event time**.

---

### 5) Triggers: *when* a window emits output

Windowing decides *where* an element belongs. **Triggers** decide *when* to output results.

Common trigger families:

* **Event-time trigger (default)**: emit when the **watermark** passes the end of the window (i.e., “data is complete enough”).
* **Processing-time trigger**: emit based on real clock time (e.g., every 30 seconds).
* **Data-driven trigger**: emit after seeing N elements (e.g., every 25 events).
* **Composite triggers**: combine rules (e.g., early outputs + final on watermark + late firings).

Practical pattern:

* **Early**: low latency (fast partial results)
* **On-time**: when watermark says window is complete
* **Late**: update results if late events arrive (within allowed lateness)

---

### 6) Accumulation modes: what each firing contains

If a trigger fires multiple times for the same window, you choose how results accumulate:

* **Accumulating**: each firing includes **all elements so far** (recomputed total).

    * Pros: easy semantics (“latest total”)
    * Cons: more state/output; sinks must handle updates/duplicates carefully.
* **Discarding**: each firing includes **only new elements since last firing** (“delta”).

    * Pros: less state; efficient for wide windows
    * Cons: downstream must sum deltas to get final totals.

Rule of thumb:

* If your aggregation is **associative + commutative** (sum, count), discarding + downstream aggregation can work well.
* If you need “the latest full value”, accumulating is simpler but heavier.

---

### 7) Dataflow monitoring metrics mentioned

You can infer backlog vs processing slowdowns using:

* **Data freshness** (linked to watermark / oldest unprocessed event time):

    * Increasing freshness ⇒ input is “older” relative to now (backlog).
* **System latency**:

    * Time for an element to fully process (including source waiting).

Interpretation:

* Freshness ↑ and latency stable ⇒ **input spike/backlog** (may autoscale workers).
* Latency ↑ and freshness stable ⇒ **processing got slower** (complex compute or slow external calls).
* Latency ↑ without CPU ↑ can indicate **external dependency** bottleneck (autoscaling may not help).

---

## Lab mapping (what you’re expected to implement)

### Batch (file → BigQuery)

* Read events.json from GCS
* Parse into structured objects/Rows
* **By user**: `Group.byFieldNames("userId")` + aggregates (Count/Sum/Max/Min)
* **By minute**:

    * attach timestamps (`WithTimestamps` / custom timestamp attach)
    * window into **FixedWindows(60s)**
    * count per window (`Combine.globally(Count.combineFn()).withoutDefaults()`)
    * rebuild `Row` with window boundary time + count, then write to BigQuery

### Streaming (Pub/Sub → BigQuery)

* Read from Pub/Sub (optionally use a timestamp attribute for event time)
* Parse JSON to objects
* Window into 1-minute fixed windows (parameterised)
* Count per window, convert to Row, write aggregated table
* Separate branch writes raw events + (event_time, processing_time)
* Visualise lag (event vs processing time scatter plot) and introduce lag via generator script

---

## Quiz 2 answers (with the “why”)

1. **What are the types of windows that you can use with Beam?**
   ✅ **Fixed, sliding, and session windows.**

2. **How does Apache Beam decide that a message is late?**
   ✅ **The option referencing the watermark is the right concept.**
   More precise wording: an element is **late when it arrives after the watermark has progressed past that element’s event time / window end**.

3. **What can you do if two messages arrive at your pipeline out of order?**
   ✅ **You can recover the order of the messages with a window using event time.**

4. **How many triggers can a window have?**
   ✅ **As many as we set.** (via composite triggers / repeated firings)
