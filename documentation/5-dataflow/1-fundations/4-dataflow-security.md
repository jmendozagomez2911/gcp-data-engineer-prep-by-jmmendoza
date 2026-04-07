# 🔐 Module — Dataflow Security: Data locality, Shared VPC, Private workers, CMEK + Lab

This module exists because “running a pipeline” is **not just compute**. A Dataflow job has:

* a **control plane** (Dataflow service backend that schedules/monitors the job),
* a **data plane** (your worker VMs that run your code),
* **networks** (how workers reach sources/sinks),
* **storage surfaces** (staging/temp buckets, persistent disks, backend state).

Security/compliance questions usually boil down to: **where does control-plane metadata go, what networks can workers talk to, and how is data encrypted at rest?**

---

## 1) 🧠 Core mental model: who talks to what

### A) Two places your job “lives”

1. **Your project (data plane)**: Compute Engine VMs (workers) run your Beam code and perform I/O.
2. **Google-managed Dataflow backend (control plane)**: validates job, orchestrates work items, autoscaling, and UI-visible metadata.

### B) Three identities matter (exam bait)

Even if you run “one job”, IAM is checked at multiple hops:

1. **Submitting user** – can you create/cancel/view jobs?
2. **Dataflow service account (service agent)** – Dataflow control plane acting against your project (create workers, check quota, etc.). ([Google Cloud Documentation][1])
3. **Worker/controller service account** – attached to worker VMs; used for reading/writing BigQuery/PubSub/GCS/etc. ([Google Cloud Documentation][1])

**Decision rule (exam trigger):**
If something fails “before workers start” → suspect **user role** or **Dataflow service agent**.
If it fails “when reading/writing data” → suspect the **worker/controller service account** permissions.

---

## 2) 🗺️ Data locality via regional endpoints (control-plane locality)

### Why it exists

Some orgs need to keep **job metadata** inside a region/country (compliance), and you also want to avoid unnecessary **inter-region network egress**.

### What it actually does (important subtlety)

Setting a Dataflow **regional endpoint** controls where the **control-plane endpoint** is located (the place workers report to / get work from). The metadata that can flow there includes health checks, work item requests/responses, work item status, autoscaling events, and error/exception info shown in the UI. ([Google Cloud Documentation][2])

**Critical boundary:** your **application data** (the actual records) stays where your sources/sinks/workers are. This feature is about **metadata + control plane**, not magically relocating your data. (The course calls this out explicitly; treat it as exam nuance.)

### How you configure it

* **Use `--region`** to pick the regional endpoint.
* If you also need a specific worker placement:

    * `--worker_zone` (pin workers to a zone inside that region)

### Exam triggers

* If you see: **“data residency / compliance / bank regulation / keep metadata in-country”** → choose **regional endpoint**.
* If you see: **“unexplained egress costs / workers in one region but job endpoint elsewhere”** → align the **endpoint region** with your pipeline region (as much as possible). ([Google Cloud Documentation][2])

### Real-world gotchas

* People confuse **endpoint region** with **where data is stored**. It’s not the same.
* If workers are in region A and endpoint is region B, you can incur extra charges/latency for **metadata chatter**, even if your data never leaves region A. ([Google Cloud Documentation][2])

---

## 3) 🕸️ Shared VPC (centralised network control)

### Why it exists

In large orgs you often want:

* **central network governance** (subnets/routes/firewalls) in a **host project**
* application teams running workloads in **service projects**

Shared VPC enables that split while letting Dataflow workers live in the right subnets. ([Google Cloud Documentation][3])

### What you must remember (exam + ops)

* Dataflow can run in:

    * a network in the **same project**, or
    * a network in a **host project** (Shared VPC). ([Google Cloud Documentation][3])
* The **Dataflow service account** must have permissions to use the host project network/subnet (commonly **Compute Network User** on the host project or subnet). ([Google Cloud Documentation][3])
* You must plan **IP capacity** in the chosen subnet.

#### The /29 example (why “only a few workers?”)

A `/29` subnet has 8 IPs; in Google Cloud, **4 are reserved** in the primary range, leaving **4 usable** addresses. So if nothing else is running there, you can launch at most **4 workers** in that subnet. ([Google Cloud Documentation][4])
This is exactly the kind of “why did scaling stop at N?” issue you see in real deployments.

### Exam triggers

* If you see: **“central network team owns VPC; workload teams are separate projects”** → Shared VPC.
* If you see: **“job fails to launch: cannot use subnet / missing network permissions”** → check **host project IAM** for Dataflow service account. ([Google Cloud Documentation][3])

---

## 4) 🚫 Disable external IPs (private workers)

### Why it exists

* Reduce attack surface: workers **cannot reach the public internet**.
* Also reduces usage of “in-use external IP” quota/cost.

### What changes when you disable public IPs

Workers can only access:

* resources in the same VPC,
* a peered VPC,
* or a Shared VPC network.

To still call Google APIs (GCS, Pub/Sub, BigQuery, etc.) from a custom network **without public IPs**, you typically need:

* **Private Google Access (PGA)** enabled on the subnet, **or**
* **Cloud NAT** (if you truly need general internet egress). ([Google Cloud Documentation][5])

### How you configure it (two common interfaces)

**Apache Beam / pipeline options** use flags like:

* `--network` / `--subnetwork`
* `--no_use_public_ips` (or conversely `--use_public_ips`) ([Google Cloud Documentation][5])

**gcloud template runs** often use:

* `--disable-public-ips` (as in your lab)

Same concept: **private-only workers**.

### Exam triggers

* If you see: **“no internet egress / private workers / compliance requires no public IPs”** → disable external IPs.
* If you see: **“private workers can’t access GCS/BigQuery/PubSub”** → check **PGA** or **Cloud NAT**. ([Google Cloud Documentation][5])

### Real-world gotchas

* The job can fail *after submission* because workers can’t download staged artifacts or call APIs.
* Turning off public IPs does **not** magically break monitoring/admin: Dataflow control-plane still manages workers; the limitation is primarily worker egress. ([Google Cloud Documentation][5])

---

## 5) 🔑 CMEK (Customer-Managed Encryption Keys)

### Why it exists

Default encryption at rest uses **Google-managed keys**. CMEK lets you use **your own Cloud KMS symmetric key** for encryption at rest for supported Dataflow storage surfaces. ([Google Cloud Documentation][6])

### What CMEK covers in Dataflow (key exam list)

During a job lifecycle, Dataflow uses:

* **Staging bucket**: pipeline binaries/artifacts
* **Temp bucket**: temp files
* **Persistent disks** on workers: disk-based shuffle and streaming state (when not fully offloaded)
* **Backend state** for **Shuffle Service** (batch) and **Streaming Engine** (streaming) when those features are used ([Google Cloud Documentation][6])

CMEK can be applied to those data-at-rest locations. ([Google Cloud Documentation][6])

### What CMEK does *not* cover (common trap)

**Job metadata** (job name, parameters, pipeline graph, job IDs, worker IPs, etc.) remains encrypted with Google-managed encryption. ([Google Cloud Documentation][6])

### IAM prerequisites (very exammy)

To use CMEK, **both** must have KMS encrypt/decrypt permissions:

* **Dataflow service account (service agent)**
* **Controller/worker service account**
  Role: **Cloud KMS CryptoKey Encrypter/Decrypter**. ([Google Cloud Documentation][6])

### Configuration rules that break jobs

* **Key region must match Dataflow job region** (regional keys only; global/multi-region keys won’t work for this setup).
* **Temp bucket region must match key region**.
* If you override workers into a different region than the key’s region, the key won’t work. ([Google Cloud Documentation][6])

**Decision rule (exam trigger):**
If you see: **“CMEK required / customer-managed keys / regulatory encryption control”** → configure CMEK *and* align **regions** for key + job + buckets, and grant KMS permissions to both service accounts. ([Google Cloud Documentation][6])

---

## 6) 🧾 Exam cheat table: requirement → feature → what to check

| If the question says…                           | Use…                                  | Key checks / gotchas                                                                                                                                |
| ----------------------------------------------- | ------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| “Metadata must stay in-country / compliance”    | **Regional endpoint (data locality)** | It’s about **control-plane metadata**, not your records; align endpoint + worker region to reduce egress/latency. ([Google Cloud Documentation][2]) |
| “Network owned by central team in host project” | **Shared VPC**                        | Dataflow service account needs **network user** access in the **host** project/subnet. ([Google Cloud Documentation][3])                            |
| “No public internet from workers”               | **Disable public IPs**                | Need **PGA** for Google APIs or **Cloud NAT**; otherwise workers can’t reach GCS/BigQuery/etc. ([Google Cloud Documentation][5])                    |
| “Customer-managed encryption keys”              | **CMEK**                              | KMS role for **service agent + worker SA**; key/job/bucket **same region**; metadata still Google-encrypted. ([Google Cloud Documentation][6])      |

---

# 🧪 Lab — Setup IAM and Networking for your Dataflow Jobs (what it’s teaching)

This lab is intentionally “simple pipeline, hard environment”:

* the pipeline is a stock **Word Count template**,
* the learning is **IAM + networking constraints**.

## What you build (end-to-end flow)

1. Create a **GCS bucket** (staging + outputs).
2. Launch a **Dataflow template job** reading from a public text file and writing results to your bucket.
3. Launch another job with **private-only workers**.

## Resources created

* Cloud Storage bucket: `gs://$PROJECT`
* Dataflow template jobs: `job1`, `job2`
* Compute Engine worker VMs (ephemeral; deleted after job success)

## IAM failures + fixes (the whole point)

### Failure #1: launching job fails (permissions)

* First attempt fails because your **user** lacks permission to run the job.
* Fix: grant yourself `roles/dataflow.admin` then rerun.
  This reinforces: **submission permission is separate from worker permission**.

### Failure #2: private-only workers fail (network access)

* Run with `--disable-public-ips` → job fails because subnet cannot reach Google APIs.
* Fix sequence:

    1. grant `roles/compute.networkAdmin` to modify subnet settings
    2. enable **Private Google Access** on the subnet
    3. rerun the job
* Verification: worker VM has **no external IP**.

**Concept the lab is burning in:**

> Turning off public IPs doesn’t just “harden security” — it changes **how workers reach GCS/BigQuery/PubSub**. Without PGA or NAT, your pipeline can’t even bootstrap.

---

## 7) ✅ High-yield takeaways to memorise

1. **Data locality is control-plane metadata locality**, not “my dataset moved regions.” ([Google Cloud Documentation][2])
2. **Shared VPC problems are solved in the host project IAM**, not your service project. ([Google Cloud Documentation][3])
3. **Private workers need a path to Google APIs** (PGA) or to internet (NAT). ([Google Cloud Documentation][5])
4. **CMEK is region-sensitive** and requires KMS permissions for **both** Dataflow service agent and worker SA; **job metadata isn’t CMEK-encrypted**. ([Google Cloud Documentation][6])

[1]: https://docs.cloud.google.com/dataflow/docs/guides/troubleshoot-permissions?utm_source=chatgpt.com "Troubleshoot Dataflow permissions"
[2]: https://docs.cloud.google.com/dataflow/docs/concepts/regional-endpoints "Dataflow regions  |  Google Cloud Documentation"
[3]: https://docs.cloud.google.com/dataflow/docs/guides/specifying-networks "Specify a network and subnetwork  |  Cloud Dataflow  |  Google Cloud Documentation"
[4]: https://docs.cloud.google.com/vpc/docs/subnets?utm_source=chatgpt.com "Subnets | Virtual Private Cloud"
[5]: https://docs.cloud.google.com/dataflow/docs/reference/pipeline-options "Pipeline options  |  Cloud Dataflow  |  Google Cloud Documentation"
[6]: https://docs.cloud.google.com/dataflow/docs/guides/customer-managed-encryption-keys "Use customer-managed encryption keys  |  Cloud Dataflow  |  Google Cloud Documentation"
