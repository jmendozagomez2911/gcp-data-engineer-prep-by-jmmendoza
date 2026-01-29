# 🧠 Module — Dataflow Foundations: Beam ↔ Dataflow, Portability, Runner v2, Custom Containers, Cross-Language Transforms

This module is *not* teaching you “how to write Beam code” (that’s the next course). It’s teaching you **how to reason about Beam pipelines as portable artifacts**, and how Dataflow fits into that picture as a managed runner that optimises execution at scale.

---

## 1) 🎯 Why this part exists in the pipeline journey

Up to now, you used Dataflow as “the streaming brain” of your architecture. The exam (and real life) then hits you with questions like:

* “Can we write in Python but still use the best Kafka connector?”
* “Can we migrate the same pipeline from on-prem to cloud without rewriting?”
* “How do we control the worker runtime (dependencies, OS libs) safely?”
* “Why does this pipeline behave differently across runners/languages?”

This module exists to give you the **mental model** that answers those questions:

### Big idea

✅ **Apache Beam defines the pipeline** (portable definition).
✅ **A runner executes it** (Dataflow is one runner).
✅ **Portability is the mechanism that makes language/runner mixing possible**.

---

## 2) 🧠 Core concepts (definitions + mental models)

### 🧩 Apache Beam (what it is)

**Apache Beam** is an open-source **unified programming model** for **batch and streaming** pipelines.

* You write a pipeline using a **Beam SDK** (Java, Python, Go, SQL, etc.).
* The *same conceptual classes/abstractions* can represent batch and streaming sources.
* The pipeline definition is independent of where it runs.

**Mental model:** Beam is the *blueprint*.

---

### 🏃 Runner (what it is)

A **runner** is the execution engine chosen to run the Beam pipeline.

* You can run locally (for dev), on a VM, or on a managed service.
* Each runner has its own config and backend.

**Mental model:** Runner is the *factory that builds from the blueprint*.

---

### ☁️ Dataflow as a runner (what it adds)

**Dataflow** is Google Cloud’s fully managed runner for Beam pipelines.

From the transcript, the “why customers value it” is operational, not conceptual:

* automated provisioning/management of resources
* **autoscaling**
* **dynamic work rebalancing** (keeps workers busy, reduces skew impact)
* integrates with Google Cloud logging/monitoring

**Mental model:** Dataflow is the *managed factory* that optimises throughput and cost while you focus on pipeline logic.

---

## 3) 🌍 Beam Portability Framework (the real focus)

### What portability is (first principles)

Portability exists because historically:

* Beam pipelines were tied tightly to the SDK language + runner specifics.
* Runners didn’t consistently support all SDKs equally.

Portability solves that by introducing a **language-agnostic representation** of pipelines and standardised communication between:

* **SDKs** (where you author the pipeline)
* **Runners** (where it executes)

### Portability API (the interoperability layer)

The module calls the interoperability layer the **Portability API**:

* well-defined, language-neutral data structures and protocols
* enables “SDK of your choice” + “runner of your choice”
* makes it feasible for “every runner to work with every supported language” (vision)

**Exam nuance to remember:** portability is not just “marketing about no lock-in” — it’s **a concrete interoperability mechanism** (protocols + representation) that makes multi-language execution possible.

---

## 4) 🧱 Container environments (why they matter in portability)

Portability depends heavily on **containerisation** because you need a consistent runtime on worker nodes.

### ✅ What containerisation gives you

* **Hermetic worker environment** (isolated from other runtimes)
* You can include **arbitrary dependencies**
* **Ahead-of-time installation** (reduce runtime surprises)
* Each user operation can be associated with an **environment** in which it executes

**Mental model:** Portability is “pipeline representation + standard protocols”.
Containers are “how you guarantee the runtime behaves the same”.

---

## 5) 🏎️ Dataflow Runner v2 (exam trigger)

The transcript is explicit:

> To use portability features, you must use **Dataflow Runner v2**.

Runner v2:

* uses a more efficient, portable work architecture based on Beam portability
* supports:

    * **custom containers**
    * **multi-language pipelines**
    * **cross-language transforms**
* is packaged with **Dataflow Shuffle service** and **Streaming Engine** (covered next module)

### Decision rule (exam style)

* If the requirement mentions **portability features** (custom containers, multi-language, cross-language transforms) → **Runner v2**.

---

## 6) 🧰 Custom containers: what you actually do (and what breaks)

### When you need a custom container

Use it when the default Beam runtime image is not enough:

* system libraries (e.g., specialised compression libs)
* pinned dependency versions
* private wheels / internal packages
* reproducible builds / consistent prod runtime

### Practical steps (what the course expects you to know)

1. Create a **Dockerfile** using the Apache Beam base image as parent.
2. Add your dependencies/customisations.
3. Build the image and push it to a container registry (e.g., `gcr.io`) using:

    * Cloud Build, or
    * Docker CLI
4. Launch the Dataflow job referencing normal params + **custom container image URI**.

### Gotchas (real-life failure modes)

* **Beam SDK version gate:** custom containers require **Beam SDK 2.25.0+** (per transcript).
  *Exam cue:* if they say “custom containers not working” → check Beam version.
* **Local testing requires Docker installed** (if you want to run locally).
* Registry/permissions issues are common:

    * job can start but workers fail to pull image (missing permissions / wrong registry host / tag).

---

## 7) 🔀 Cross-language transforms (why they exist and how they work)

### The problem they solve

Beam SDKs don’t always have feature parity. Historically:

* some I/O connectors existed only in Java
* Python users were blocked or had to rewrite components

Portability changes that: **a single pipeline can execute transforms written in different languages**.

### What a cross-language transform is

A transform authored in one language (often Java) that can be used from a pipeline authored in another (e.g., Python).

The example in the transcript:

* Python pipeline uses `ReadFromKafka` from `apache_beam.io.kafka`
* but that transform is **implemented in the Java SDK**

### What happens under the hood (high-yield mechanics)

* The Python SDK starts a **local Java service** to create/inject Java pipeline fragments (think “expansion service” conceptually).
* It **downloads** the Java dependencies needed to execute the transform.
* At runtime, Dataflow workers execute **Python and Java code simultaneously**.

### Gotchas (what breaks)

* Cross-language transforms can introduce:

    * additional dependency download time
    * version mismatches between SDK/runtime expectations
    * “works locally but fails on Dataflow” if container/runtime differs
* If your pipeline suddenly requires Java artefacts while you’re “in Python”, debugging gets trickier (you must think multi-runtime).

### Decision rule (exam style)

* If the question says: “Python pipeline needs a connector only available in Java” → **cross-language transform via portability**.
* If the question says: “need a controlled runtime for dependencies” → **custom containers + Runner v2**.

---

## 8) 🧠 Exam cheats: pick-the-tool cues + common traps

### High-yield cues

* “Write once, run on any runner / any SDK” → **Beam portability framework**
* “Language-agnostic representation + protocols between SDK and runner” → **Portability API**
* “Custom containers / multi-language / cross-language transforms” → **Dataflow Runner v2**
* “Need Kafka IO in Python but only Java has the mature connector” → **cross-language transform**
* “Need hermetic runtime with custom deps” → **containerised Beam environment**

### Common traps

* **Trap:** “Portability = hermetic worker environment”
  → Not exactly. Hermetic env comes from **containerisation**, portability is broader (representation + protocols).
* **Trap:** “Cross-language transforms = portability framework”
  → Cross-language transforms are a **benefit enabled by portability**, not the definition of the framework itself.
* **Trap:** “Dataflow = Beam”
  → Beam defines; Dataflow executes (runner). Don’t mix blueprint and factory.

---

# ✅ Quiz (integrated with reasoning)

## Q1) Benefits of Beam portability (Select ALL that apply)

Options:

* Cross-language transforms
* Running pipelines authored in any SDK on any runner
* Implement new Beam transforms using a language of choice and utilise these transforms from other languages

✅ **Correct:** **All of the above**

**Why:**

* Portability enables **cross-language transforms** by standardising representation and environments.
* The vision is “**any SDK on any runner**” via the portability interoperability layer.
* Once transforms can be represented language-agnostically, you can implement in one language and **use from others**.

---

## Q2) What is the Beam portability framework? (Single best answer)

Options:

* A set of protocols for executing pipelines
* A hermetic worker environment
* A language-agnostic way to represent pipelines
* A set of cross-language transforms

✅ **Correct:** **A language-agnostic way to represent pipelines**

**Why this is the best choice:**

* The transcript defines portability as a **language-agnostic way of representing and executing** Beam pipelines, with protocols/structures between SDKs and runners.
* “Protocols for executing pipelines” is *part* of it (Portability API), but the framework’s defining feature is the **language-neutral representation**.

❌ Why the others are wrong:

* “Hermetic worker environment” → that’s primarily **containerisation**, which supports portability but isn’t the definition.
* “A set of cross-language transforms” → those are a **capability enabled by portability**, not the framework itself.
