# Dataflow Performance — full explanation

This module is about one central idea:

> **A Dataflow pipeline does not become fast just because you add more workers.**

Performance depends mainly on four things:

1. **How the pipeline is designed**
2. **What the data looks like**
3. **How the pipeline interacts with external systems**
4. **Which Dataflow-specific optimisations are enabled**

That is the correct mental model for this lesson.

---

# 1. Pipeline design decisions

## Filter early

One of the simplest but most important rules is:

> **Reduce data volume as early as possible.**

If a transform removes unnecessary records, columns, or payload, place it near the top of the pipeline graph.

Why this matters:

* fewer elements continue through the pipeline
* less work for later transforms
* less network traffic
* less serialisation and deserialisation
* less shuffle cost
* lower CPU and memory usage

The transcript even says this should be done **above windowing operations** where possible.

Why above windowing?

Because although `Window` does not do heavy computation by itself, it prepares data for later aggregations. If you can shrink the dataset before that, every later stage benefits.

### Real meaning

Do not think:

> “I will filter later once I have already grouped or windowed the data.”

Think:

> “Anything I can discard early should disappear immediately.”

---

## Use efficient coders

The module highlights that **encoding and decoding overhead is expensive**.

In Beam, data moves between transforms, workers, and storage layers. That means objects are repeatedly encoded and decoded. If your coder is inefficient, your pipeline pays that cost again and again.

In Java, the course explicitly warns against using:

* `SerializableCoder`

and recommends more efficient options such as:

* `ProtoCoder`
* schema-based encoding

### Why `SerializableCoder` is bad

`SerializableCoder` uses Java serialisation, which is flexible but often slow and inefficient. It usually produces more overhead than specialised coders.

### Why better coders help

Efficient coders:

* reduce CPU overhead
* reduce bytes transferred
* improve worker throughput

---

## Selective decoding

This is a more subtle performance idea.

If a record contains a large blob but you only need part of it, do **not** deserialize the whole thing unnecessarily.

The example mentioned is Protobuf `FieldMask`.

### Core idea

If you only need 2 fields out of 50, decode only those 2 if your format and tooling allow it.

That matters because deserialising large payloads repeatedly can become a major hidden cost.

### Interview-style takeaway

A pipeline may look computationally simple, but still be slow because it is spending time **encoding and decoding large objects**.

---

## Pre-aggregate before large sliding windows

This is an important design optimisation.

If you have:

* large windows
* lots of data
* sliding window aggregations

then repeatedly reprocessing all raw elements for each window movement can be expensive.

The recommendation is:

> create smaller **Window + Combine** patterns before the main sliding window

### Why this helps

A sliding window overlaps with previous windows. That means many elements are reconsidered multiple times.

If you first aggregate smaller chunks, the final sliding window works over **partial aggregates** instead of raw records.

### Practical meaning

Instead of repeatedly scanning millions of raw events, the pipeline can process already condensed results.

This reduces:

* element volume
* shuffle cost
* aggregation work

---

## Fusion: good by default, bad in some cases

Dataflow can optimise your graph by **fusing multiple transforms together** into one execution stage.

In general, fusion is helpful because it can reduce overhead between stages.

But the lesson says there are situations where you **do not want fusion**.

---

## When fusion becomes a problem: large fanout

A **fanout transformation** is when one input element produces many output elements.

For example:

* one input record generates hundreds or thousands of outputs

This is the main scenario the module highlights.

### Why fusion is bad here

If the large-fanout transform stays fused with downstream work, the same stage may become overloaded:

* one worker expands huge outputs
* downstream processing gets tied to that same execution path
* the pipeline may lose parallel efficiency

### How to prevent fusion

The transcript gives two methods:

#### 1. Insert a `Reshuffle`

This forces rematerialisation and breaks fusion.

#### 2. Use side inputs

If you pass an intermediate `PCollection` as a side input to another `ParDo`, Dataflow materialises it.

### Important idea

You do **not** avoid fusion everywhere.

You avoid it **only when fusion makes execution worse**, especially with massive fanout.

So the correct exam idea is:

> fusion is usually good, but not always

---

## Too much logging can destroy performance

This part is extremely practical.

The module says one of the most common causes of performance tickets is simply:

> **too much logging**

In Dataflow, logs from all workers are sent to a central logging system.

Now imagine:

* hundreds or thousands of workers
* each producing many logs per second

That can create back pressure and waste resources.

### What should be avoided

The transcript is very clear:

> `Log.info` at element granularity should almost always be avoided

Meaning: do not log once per element in a `PCollection`.

That is usually useless and very expensive.

### Why

Because the cost is multiplied across the entire dataset and all workers.

### Better alternative

For data-quality problems, the module suggests:

* a **dead-letter pattern**
* followed by a **count per time window**

That gives observability without flooding logs.

### Real lesson

Logging is not free. In distributed systems, bad logging can become a bottleneck.

---

# 2. Effects of data shape on performance

This section says performance is not only about code. It is also about **how the data is distributed**.

---

## Data skew

Data skew means the work is not evenly distributed.

This becomes especially visible in operations like:

* `GroupByKey`
* `CombinePerKey`

Because values for the same key must end up together.

### Why this matters

If one key has far more data than others, one worker may end up doing most of the work while others stay underused.

That key becomes a **hot key**.

### Example

If a nullable column is used as a key, many records may share the same null-like value. That can create one giant group.

---

## Hot key mitigation techniques

The transcript gives three main ideas.

### 1. `withFanout(int)`

This creates intermediate aggregation levels before the final combine.

Instead of sending everything for a hot key directly into one final combine step, work is partially spread out first.

### 2. `withHotKeyFanout(...)`

This is similar, but more flexible. It allows the fanout behaviour to depend on the key.

That is useful when only some keys are problematic.

### 3. Use Dataflow Shuffle / Streaming Engine

These services offload shuffle/state work to backend services rather than keeping everything constrained on one worker machine.

This helps with scalability and makes hot-key situations easier to handle.

---

## `hotKeyLoggingEnabled`

This is a debugging aid.

If you enable:

* `hotKeyLoggingEnabled = true`

Dataflow can log the **actual key** causing the bottleneck.

Without that flag, it may tell you a hot key exists but not reveal which one.

### Why this matters

Once you know the exact key, you can design custom logic for it.

For example:

* special partitioning
* special fanout
* special routing
* special pre-aggregation

This is not a theoretical detail. It is operationally useful.

---

## Key space matters

This is one of the most important performance concepts in distributed grouping.

### Too few keys

Bad for performance.

Why?

Because the amount of parallelism is limited by how many independent key groups exist.

If there are only a few keys, you cannot spread the workload across many workers effectively.

Even if you add more workers, they may have nothing useful to do.

---

### Too many keys

Also not automatically good.

If key cardinality becomes extremely large, overhead can increase:

* more bookkeeping
* more partitioning metadata
* more coordination cost

So the point is not:

> “More keys is always better.”

The real point is:

> “Key space should support parallelism without creating excessive overhead.”

---

## Hashing keys internally

If the key space is very large, or if keys include date/time information, the lesson suggests hashing or separating keys internally.

The idea is to shape key distribution so work can be reused and balanced better.

### Why date/time in keys matters

If keys are tied to time, old keys may become inactive and effectively “free up” processing space for new ones.

This can help maintain manageable key distribution.

---

## Add window to the key when windows are distinct

This is a subtle but important optimisation.

If windows are distinct, then adding the window as part of the key can improve parallelism.

Why?

Because records from different windows can now be treated as unrelated groups.

That allows more workers to process them independently.

### Core idea

A key alone may be too coarse.
A `(key, window)` pair may expose more parallel work.

---

# 3. External systems can become the bottleneck

This section says:

> Even if Dataflow itself is fast, your pipeline can still be slow because of the systems around it.

That is a very common real-world problem.

---

## Gzip files with `TextIO`

This is one of the clearest examples in the transcript.

If you read **gzip files** with `TextIO`, those files cannot be read in parallel.

### Why

Gzip compression is not splittable in the same way as some other formats.

So:

* one file is handled by one thread
* that reduces read parallelism

### Negative consequences

The transcript lists three:

#### 1. Only one machine performs the read

So the read stage becomes limited.

#### 2. Fused downstream stages stay on that same worker

Because of fusion, later work may remain tied to the worker that read the file.

#### 3. Shuffle becomes bottlenecked by that machine

One machine now has to push all file data into the rest of the distributed system.

So the network of that single host becomes the bottleneck.

### Recommended alternatives

* use uncompressed files with `TextIO`
* or use compressed **Avro** instead

The underlying message is:

> file format choice affects parallelism

---

## External systems can be overwhelmed

Beam runners can generate a lot of parallel activity.

That is good for Dataflow, but bad if your external system cannot keep up.

For example:

* database writes
* REST APIs
* legacy systems
* non-scalable sinks

### Why batch is worse

The transcript says this issue is often more pronounced in:

* batch pipelines
* backlog catch-up in streaming pipelines

That makes sense because Dataflow may try to process large amounts of pending work very quickly.

If the downstream system cannot absorb that rate, it becomes the bottleneck.

---

## How to reduce pressure on external systems

The recommendation is to use batching techniques such as:

* `GroupIntoBatches`
* `@StartBundle`
* `@FinishBundle`

### Why batching helps

Instead of one call per element, you perform fewer, larger calls.

That reduces:

* connection overhead
* request overhead
* pressure on the target system

### Also important

The transcript says you should provision the external service for peak Dataflow volume.

That means:

* do not only tune Dataflow
* also make sure the sink/source can handle the traffic Dataflow creates

---

## Colocation matters

This is another simple but high-impact rule.

Using services in the **same region** usually reduces latency.

That matters especially when Dataflow interacts heavily with services like:

* BigQuery
* Bigtable
* other cloud services

### Why this matters

Even small per-call latency becomes expensive when multiplied across many operations.

So colocation improves:

* response time
* throughput
* overall pipeline efficiency

---

# 4. Dataflow-specific optimisation options

This section is about features provided by Dataflow itself.

---

## Dataflow Shuffle

Dataflow Shuffle is the backend mechanism used for operations such as:

* `GroupByKey`
* `CoGroupByKey`
* `Combine`

These require grouping data by key, which is one of the most expensive distributed operations.

---

## Traditional shuffle vs service-based shuffle

### Traditional model

Shuffle runs on worker VMs and consumes:

* worker CPU
* worker memory
* persistent disk

### Service-based Dataflow Shuffle

For **batch pipelines only**, Dataflow can move shuffle work out of worker VMs and into the managed backend service.

---

## Benefits of Dataflow Shuffle

The transcript gives four main benefits.

### 1. Faster execution

Many batch jobs run faster because workers are no longer burdened by local shuffle management.

### 2. Less worker resource consumption

Workers use less CPU, memory, and disk.

### 3. Better autoscaling

Because workers are not holding shuffle data locally, they can scale down earlier.

### 4. Better fault tolerance

If a VM becomes unhealthy, the job is less likely to fail because critical shuffle state is not trapped on that machine.

### Key idea

Moving infrastructure-heavy work out of workers makes the whole system more elastic and resilient.

---

## Streaming Engine

For streaming pipelines, the equivalent idea is **Streaming Engine**.

It offloads:

* window state storage
* streaming shuffle-related work

from worker disks to a backend service.

### Important distinction

* **Dataflow Shuffle** → batch
* **Streaming Engine** → streaming

That distinction is easy exam material.

---

## No code changes required

One of the most important practical points:

> You do not need to rewrite your pipeline logic to benefit from these features.

Your workers still run your user code, but the heavy system work is delegated to managed backend services.

That is why these features often solve:

* scalability issues
* autoscaling issues
* resource pressure issues

without changing business logic.

---

# What the module is really trying to teach

If I strip away the wording, the module is really saying this:

## A performant Dataflow pipeline is built by:

* reducing data early
* choosing efficient representations
* avoiding pathological graph patterns
* being careful with logging
* understanding skew and hot keys
* protecting external systems
* using the right managed backend features

So performance is not one single trick. It is the combination of:

* **good graph design**
* **good data distribution**
* **good system interaction**
* **good platform configuration**

---

# Exam-focused notes

## 1. When should fusion be avoided?

Not always.

Only in specific cases, especially **massive fanout**.

Why?
Because fusion can keep too much work tied together and reduce effective parallelism.

How to avoid it?

* `Reshuffle`
* materialised side inputs

---

## 2. How do you mitigate data skew / hot keys?

Main answers:

* `withFanout`
* `withHotKeyFanout`
* Dataflow Shuffle for batch
* Streaming Engine for streaming

Adding more workers alone is **not** the real fix if the key distribution is the problem.

---

## 3. Why are gzip files a problem with `TextIO`?

Because they are not read in parallel efficiently.

That creates:

* read bottleneck
* fused-stage bottleneck
* shuffle bottleneck

---

## 4. Why is logging a performance risk?

Because every worker sends logs centrally.
At scale, excessive per-element logging creates back pressure and wasted resources.

---

## 5. Why do coders matter?

Because encoding/decoding is repeated constantly in Beam pipelines.
Bad coders create hidden CPU and throughput cost.

---

## 6. Difference between Dataflow Shuffle and Streaming Engine

* **Dataflow Shuffle**: batch pipelines
* **Streaming Engine**: streaming pipelines

Both move heavy backend work away from workers.

---

# Quiz answers with explanation

## 1. When should we avoid fusion in a Dataflow pipeline?

**Correct answer:**

* **Only in specific scenarios, like if your pipeline involves massive fanouts.**

Why?
Because fusion is generally beneficial, but can hurt performance in fanout-heavy situations.

---

## 2. Select options we can use to mitigate data skew in Dataflow pipelines

**Correct answers:**

* **Use Dataflow shuffle for batch pipelines and Dataflow streaming option for streaming pipelines.**
* **Use APIs like `withFanout` or `withHotKeyFanout`.**

Why not the others?

* Adding more workers does not solve skew if one key is still dominating.
* Composite windows and triggers are not the mitigation the module teaches for skew.

---

## 3. Which one of the following is not a consideration for designing performant pipelines in Dataflow?

**Correct answer:**

* **SDK used for developing the pipeline.**

Why?
The module’s design considerations were:

* filtering early
* coders/decoders
* logging
* fusion/fanout patterns

It did **not** frame SDK choice itself as the design-performance factor here.

---

# Final mental model

If this came up in an interview, the strongest compact answer would be:

> Dataflow performance depends less on raw worker count and more on pipeline shape, data distribution, external bottlenecks, and platform features. The key practices are filtering early, using efficient coders, avoiding excessive logging, handling skew and hot keys properly, protecting external systems with batching, and enabling Dataflow Shuffle or Streaming Engine when appropriate.

