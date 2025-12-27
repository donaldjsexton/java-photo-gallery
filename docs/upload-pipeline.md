# Upload & Ingestion Pipeline --- Java Photo Gallery

## Purpose of This Document

This document describes the ingestion pipeline step‑by‑step from upload
request through persistence and logging. It is written to support:

- engineers inheriting or extending the system
- reviewers evaluating reliability and operational maturity
- future sprints that introduce queues, batching, or storage adapters

The design goal is to ensure ingestion is:

- deterministic\
- restart‑safe\
- idempotent\
- observable

---

## Pipeline Objectives

The ingestion workflow is engineered to:

1. Prevent unnecessary storage growth
2. Allow safe re‑attempts after partial failure
3. Preserve data integrity at every stage
4. Separate metadata failure from ingestion success
5. Keep system state explainable and auditable

Uploads should never require manual cleanup or undefined behavior.

---

## End‑to‑End Flow (Narrative)

```ini
Client Upload
   │
   ▼
Ingestion Endpoint
   │
   ▼
Content Hashing
   │
   ├─ If hash exists → Duplicate Handling Path
   │   ├─ Skip storage write
   │   └─ Re‑associate with gallery if needed
   │
   └─ If hash does not exist → New Upload Path
           │
           ▼
       Validation
           │
           ▼
   Persist Image Record
           │
           ▼
   Attempt Metadata Extraction
           │
           ├─ Success → Store metadata
           └─ Failure → Store ingest success + partial status
           │
           ▼
   Gallery Association
           │
           ▼
   Processing Result Logged
```

Every stage is designed to complete **without leaving invalid state
behind**.

---

## Stage 1 --- Ingestion Request

Inputs may originate from:

- web client
- local upload tool
- scripted ingestion pipeline (future)

Initial validation ensures:

- file exists
- file meets minimum type requirements
- ingest request is structurally valid

At this stage, no permanent writes occur.

---

## Stage 2 --- Content Hashing (Idempotency Gate)

A hash of the file contents is generated before persistence.

Purpose:

- detect duplicates across sessions and users
- prevent redundant storage growth
- enable safe retry behavior

Outcomes:

- **Hash already exists**
   - ingestion does *not* expand storage
   - system may re‑associate with gallery if appropriate
   - upload is treated as *repeat event*, not new data

- **Hash is new**
   - upload continues through normal processing path

This turns ingestion into a **deterministic workflow**.

---

## Stage 3 --- Validation & Persistence

Validation occurs before persistence expansion, including:

- structural validation
- storage reference generation
- gallery reference readiness

Once validated:

- an `Image` record is created
- storage pointer is made durable
- ingest is now recoverable

From this point forward, no action should create unrecoverable state.

---

## Stage 4 --- Metadata Extraction (Non‑Blocking)

Metadata extraction runs after persistence.

Reasons this step is intentionally **non‑blocking**:

- real‑world photos may have incomplete or malformed EXIF
- camera and editing workflows vary
- metadata may be enriched later

Design choices:

- metadata stored in separate table
- ingestion success does not depend on metadata success
- failures are recorded and auditable

This ensures ingestion never fails due to non‑critical metadata issues.

---

## Stage 5 --- Gallery Association

Images are associated with galleries **after persistence**, allowing:

- safe reassignment
- future multi‑gallery membership
- non‑destructive browsing hierarchy changes

The gallery layer is **organizational, not structural** --- uploads
remain valid regardless of UI grouping behavior.

---

## Stage 6 --- Processing Result Logging

Each ingestion event produces an operational outcome record.

Examples of logged states:

- New upload --- processed successfully
- Duplicate detected --- skipped storage write
- Metadata missing --- upload preserved, metadata deferred
- Partial failure --- safe retry permitted

Logging supports:

- supportability
- post‑incident review
- ingestion diagnostics

The system is designed to be **explainable after the fact**.

---

## Failure Scenarios Accounted For

The pipeline explicitly tolerates:

- network interruption during upload
- user retry after timeout
- partial image corruption
- repeated duplicate uploads
- metadata parser failure
- gallery reassignment during ingest

Safety behaviors include:

- no orphaned records
- no duplicate storage expansion
- retry‑safe ingestion
- consistent database state

Failures are treated as operational signals, not breakpoints.

---

## Future Pipeline Enhancements (Sprint Candidates)

Future engineering sprints may introduce:

### Background Processing Queue

To offload heavy or batch ingestion tasks.

### Storage Adapter Layer

To support object storage / S3‑compatible systems.

### Ingestion Dashboard

Operational visibility into:

- success / failure rates
- duplicate detection events
- retry activity

### Event Publishing Hook

Allowing downstream consumers to process:

- thumbnails
- AI tagging
- content indexing

All enhancements continue the same **stability‑first ingestion
philosophy**.

---

## Pipeline Summary

The ingestion pipeline demonstrates:

- deterministic behavior
- idempotent processing
- separation of concerns
- persistence‑first reliability
- recoverable failure handling
- forward‑compatible system evolution

Uploads are not just accepted --- they are **controlled, validated, and
made durable**.