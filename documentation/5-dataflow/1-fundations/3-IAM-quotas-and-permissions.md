# 🧠 Module — Dataflow IAM, Service Accounts, and Quotas (Why jobs launch… or fail)

This part exists because **Dataflow is “managed”, not “magic.”** When you run a Beam pipeline on Dataflow, you’re actually asking a managed service to (1) accept your job, (2) stage your artefacts, (3) spin up **Compute Engine worker VMs** in your project, and (4) let those workers read/write your other resources (BigQuery, Pub/Sub, GCS, etc.).
So the core question becomes:

> **Which identity is allowed to do what, at each step—and do you have enough quota for what Dataflow wants to create?**

---

## 1) 🧩 Why this part exists in the streaming pipeline

Earlier modules covered **what** you build (Pub/Sub → Dataflow → BigQuery/Bigtable).
This module is about **what must be true operationally** for Dataflow to run at all:

* **IAM**: who can create/cancel jobs, and which service accounts can access data/services.
* **Quotas**: whether your project can actually allocate the VMs, IPs, and disks Dataflow needs—*including future autoscaling headroom*.

**Exam trigger:** if the question smells like *“PERMISSION_DENIED”*, *“quota exceeded”*, *“job won’t start”*, *“can’t scale”*, it’s this module.

---

## 2) 🚦 The “job submission” mental model: 3 gates where IAM is checked

When you submit a Dataflow job, the pipeline effectively goes through these stages:

### Gate A — Your SDK submits + stages artefacts

* The SDK (Python/Java/Go) **uploads your code and dependencies to Cloud Storage** (“staging”).
* It also sends the **pipeline definition** to the Dataflow service. ([Google Cloud Documentation][1])

✅ IAM must allow **you (the submitter identity)** to:

* create jobs (`dataflow.jobs.create`)
* stage objects to the bucket (GCS write permissions)
* (often) use a worker service account (`iam.serviceAccountUser`) ([Google Cloud Documentation][2])

### Gate B — Dataflow service validates + creates resources

Dataflow then:

* validates & optimises the pipeline graph
* creates worker VMs (managed instance group under the hood)
* begins monitoring/logging ([Google Cloud Documentation][1])

✅ IAM must allow **the Dataflow service agent** to create/manage those resources. ([Stack Overflow][3])

### Gate C — Worker VMs run and access your data

Finally, worker VMs run your code and must read/write:

* Pub/Sub / BigQuery / Bigtable / GCS / etc.

✅ IAM must allow **the worker (controller) service account** to access those services. ([Google Cloud Documentation][4])

---

## 3) 🪪 The 3 identities that decide whether a job can run

This is the core of the module.

### (1) 👤 The submitting user (your identity)

Dataflow has three common predefined roles for *Dataflow-specific actions*:

* **Dataflow Viewer**: view jobs only (no submit/cancel/update). ([Google Cloud Documentation][5])
* **Dataflow Developer**: can create and manage jobs *but* you often still need extra permissions (below). ([Google Cloud Documentation][5])
* **Dataflow Admin**: broad Dataflow control. ([Google Cloud Documentation][5])

**Critical gotcha (matches your transcript):**
Even if you have `roles/dataflow.developer`, job creation can still fail unless you also have:

* permission to **stage files in GCS** (e.g., `roles/storage.objectAdmin` on the staging bucket)
* permission to **view Compute Engine quota / machine types** (e.g., `roles/compute.viewer`)
* often `roles/iam.serviceAccountUser` to run the job using a particular worker service account ([Google Cloud Documentation][2])

**Exam trigger:**
If you see `dataflow.jobs.create` denied → missing Dataflow Developer/Admin. ([Google Cloud Documentation][2])
If you see staging bucket write errors → missing GCS permissions (usually on the *staging/temp bucket*). ([price2meet.com][6])

---

### (2) 🤖 The Dataflow **service agent** (service-controlled identity)

This is the identity Dataflow uses to “act on your behalf” to manage infrastructure in your project.

* It’s created when you enable the Dataflow API.
* It has `roles/dataflow.serviceAgent` and is typically like:
  `service-<PROJECT_NUMBER>@dataflow-service-producer-prod.iam.gserviceaccount.com` ([Stack Overflow][3])

**What it does:**

* creates/controls worker VMs (and related infra)
* checks quotas
* manages the job lifecycle ([Google Cloud Documentation][1])

**Exam trigger:**
If infra creation fails in weird ways (MIG/template/instance creation), the service agent permissions or org policies can be involved. ([Google Cloud Documentation][1])

---

### (3) 🏗️ The **worker (controller) service account** (runs on the VMs)

This is the identity the worker processes use when calling other GCP APIs.

* Default: **Compute Engine default service account**
  `<PROJECT_NUMBER>-compute@developer.gserviceaccount.com` ([Google Cloud Documentation][4])
* Recommended for prod: **create a dedicated SA** with least privilege. ([Google Cloud Documentation][4])

**Minimum role:** `roles/dataflow.worker` (so the workers can function as Dataflow workers). ([Google Cloud Documentation][4])
**Plus** whatever your pipeline needs:

* BigQuery read/write roles
* Pub/Sub subscriber/publisher
* GCS read/write, etc. ([Google Cloud Documentation][2])

**Decision rule:**
If the error happens **on workers at runtime** (e.g., BigQuery/PubSub permissions) → it’s almost always the **worker service account** missing roles. ([Google Cloud Documentation][2])

---

## 4) 📏 Quotas: why “it should work” still fails at launch time

Dataflow consumes **Compute Engine quotas** because it spins up VMs.

### A) CPU quota

* CPU quota is total vCPUs in a region.
* Machine type matters: 100 workers of `n1-standard-8` ⇒ 800 vCPU required. ([Google Cloud Documentation][1])

**Exam trigger:** “quota exceeded” when choosing bigger machine types or large max workers. ([Google Cloud Documentation][1])

---

### B) External IP (“in-use IP address”) quota

* By default, Dataflow workers get **external + internal** IPs.
* External IPs consume quota and cost money. ([Google Cloud Documentation][1])

If you don’t need public internet:

* run with internal IPs only:

    * **Python:** `--no_use_public_ips`
    * also enable **Private Google Access** for Google APIs ([Google Cloud Documentation][7])

**Subtle gotcha:**
Even if CPU quota is fine, **external IP quota** might be the limiting factor—whichever is more restrictive blocks the job. ([Google Cloud Documentation][1])

---

### C) Persistent disk quota (and why autoscaling can “reserve” disks)

You can choose disk type:

* `pd-standard` (HDD)
* `pd-ssd` (SSD) ([Google Cloud Documentation][8])

Key defaults (high-yield):

* **Batch (no Shuffle Service):** 250 GB per worker disk
* **Batch with Shuffle Service:** 25 GB boot disk
* **Streaming (no Streaming Engine):** 400 GB per worker disk
* **Streaming with Streaming Engine:** 30 GB boot disk ([Google Cloud Documentation][1])

**The exam-bait rule (from your transcript):**

> In streaming, disk quota counts against **max_num_workers**, not current workers.
> So if `max_num_workers = 25`, quota is reserved for **25 disks**, even if you start with 3 workers. ([Google Cloud Documentation][1])

Also:

* Streaming jobs are limited to **15 persistent disks per worker** (and at least 1 per worker). ([Google Cloud Documentation][1])

---

## 5) 🧰 The main flags you’re expected to recognise

* `--max_num_workers`

    * required for streaming jobs **without** Streaming Engine
    * optional with Streaming Engine (default is often 100) ([Google Cloud Documentation][1])
* `--disk_size_gb` (override disk size) ([Google Cloud Documentation][1])
* `--worker_disk_type=compute.googleapis.com/projects/.../diskTypes/pd-ssd` (or `pd-standard`) ([Google Cloud Documentation][8])
* `--no_use_public_ips` (Python) + Private Google Access for internal-only workers ([Google Cloud Documentation][7])

---

# ✅ Quiz integration (with reasoning)

## Quiz Q1

**Your project’s current SSD usage is 100 TB.**
You launch a **streaming** pipeline with shuffle done **on the VM**.
Initial workers = 5, **max workers = 100**.
What is SSD usage when the job launches?

### Correct answer: **140 TB**

**Why:**

* Streaming **on worker VMs** defaults to **400 GB persistent disk per worker**. ([Google Cloud Documentation][1])
* Streaming disk quota allocation is based on **max_num_workers**, not initial workers. ([Google Cloud Documentation][1])
* Disk reserved = `100 workers * 400 GB = 40,000 GB = 40 TB`
* Total = existing `100 TB + 40 TB = 140 TB`

So: ✅ **140 TB**

---

## Quiz Q2

You run:
`gcloud dataflow jobs cancel <job-id> --region=<region>`
Which roles can be assigned to you for the command to work?

### Correct options: **Dataflow Admin** and **Dataflow Developer**

**Why:**

* Cancel needs `dataflow.jobs.cancel`. The Dataflow Viewer role can’t cancel. ([Google Cloud Documentation][2])
* The troubleshoot guide explicitly ties cancel permission errors to missing `roles/dataflow.developer` (and/or admin-level access). ([Google Cloud Documentation][2])

So: ✅ **Dataflow Developer**, ✅ **Dataflow Admin**

---

# 🧠 Exam decision rules to memorise

1. **Job won’t create / PERMISSION_DENIED on submit**
   → check *your identity*: `roles/dataflow.developer/admin` + **GCS staging write** + **compute.viewer** + often `iam.serviceAccountUser`. ([Google Cloud Documentation][2])

2. **Job starts but fails reading/writing Pub/Sub/BigQuery/GCS**
   → check **worker (controller) service account** roles. ([Google Cloud Documentation][2])

3. **Quota exceeded at launch or can’t scale**
   → Dataflow validates quota for **max_num_workers**, not just initial. ([Google Cloud Documentation][1])

4. **Too many external IPs / costs high / quota blocking**
   → use **internal IPs only** (`--no_use_public_ips`) + Private Google Access. ([Google Cloud Documentation][7])

5. **Disk quota blows up in streaming**
   → remember: streaming disk reservation is tied to **max_num_workers** and defaults to **400 GB/worker** on-VM. ([Google Cloud Documentation][1])


[1]: https://docs.cloud.google.com/dataflow/docs/request-quotas "Request quotas  |  Cloud Dataflow  |  Google Cloud Documentation"
[2]: https://docs.cloud.google.com/dataflow/docs/guides/troubleshoot-permissions?utm_source=chatgpt.com "Troubleshoot Dataflow permissions"
[3]: https://stackoverflow.com/questions/71436000/question-about-permissions-on-google-cloud-dataflow?utm_source=chatgpt.com "Question about permissions on Google Cloud Dataflow"
[4]: https://docs.cloud.google.com/dataflow/docs/concepts/security-and-permissions?utm_source=chatgpt.com "Dataflow security and permissions"
[5]: https://docs.cloud.google.com/dataflow/docs/concepts/access-control?utm_source=chatgpt.com "Access control with IAM | Cloud Dataflow"
[6]: https://price2meet.com/gcp/docs/dataflow_docs_concepts_access-control.pdf?utm_source=chatgpt.com "Cloud Data ow access control guide - Price 2 Meet"
[7]: https://docs.cloud.google.com/dataflow/docs/guides/routes-firewall "Configure internet access and firewall rules  |  Cloud Dataflow  |  Google Cloud Documentation"
[8]: https://docs.cloud.google.com/dataflow/docs/guides/configure-worker-vm?utm_source=chatgpt.com "Configure Dataflow worker VMs"
