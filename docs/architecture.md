# Architecture --- Java Photo Gallery

## Purpose of This Document

This document provides a clear, implementation‑level overview of how the
Java Photo Gallery is structured and why architectural decisions were
made. It is written for an engineer inheriting the project, a hiring
manager reviewing system design maturity, or a teammate preparing to
extend the platform.

The goal is to make the system **predictable, explainable, and hand‑off
ready**.

---

## System Goals & Design Principles

The platform was designed around four core engineering priorities:

1. **Reliability over features**
2. **Persistence‑first architecture**
3. **Deterministic ingestion workflow**
4. **Failure‑aware, restart‑safe behavior**

Rather than optimizing for UI or rapid iteration, the system is built as
a backend platform that can expand safely over time.

This mirrors how internal engineering systems evolve in production
environments.

---

## High‑Level System Overview

```ini
Client / Upload Source
        │
        ▼
 Ingestion Endpoint
        │
        ▼
 Hashing + Validation
        │
        ├─► Duplicate Detection
        │       ├─ Existing → Skip / Re‑associate
        │       └─ New → Continue
        ▼
 Metadata Extraction (non‑blocking)
        │
        ▼
 Persistence Layer (PostgreSQL)
        │
        ▼
 Gallery Hierarchy Association
        │
        ▼
 Processing Result Logging
```

Ingestion is **ordered, deterministic, and restart‑safe**.

Nothing is written that cannot be recovered or reasoned about later.

---

## Backend Technology Stack

Layer               Technology           Rationale

---

Language            Java                 Predictable runtime + type safety
Framework           Spring Boot          Explicit lifecycle, modular growth
Database            PostgreSQL           Relational integrity + normalized schema
Migrations          Flyway               Schema discipline and traceability
Hashing             Content hash table   Storage‑safe dedupe
Metadata Handling   Separate table       Tolerates partial extraction

This stack reinforces the **persistence‑first philosophy**.

---

## Core Domain Entities

### `Gallery`

Represents a hierarchical container for images.

Supports:

- nested browsing
- grouping by event / project
- future ACL separation

---

### `Image`

Represents the stored asset reference.

Includes:

- storage reference
- gallery membership
- core identity fields

Images do **not** depend on metadata completeness --- uploads remain
valid even when metadata fails.

---

### `ImageMetadata`

Holds extracted EXIF / supplemental data.

Design motivation:

- allows partial extraction
- tolerates missing metadata fields
- supports later enrichment

Metadata is **secondary**, persistence is primary.

---

### `UploadHash`

Stores file content hashes for duplicate detection.

Used to:

- prevent redundant storage
- support idempotent retries
- ensure deterministic ingestion behavior

Hash matching occurs **before persistence expansion**.

---

### `GalleryMembership`

Joins images into hierarchical browsing paths.

Future‑safe for:

- multi‑gallery association
- collections / albums
- role‑based sharing

---

## Why The Data Model Is Structured This Way

Typical student or hobby projects:

- tightly couple images + metadata
- assume all fields exist
- fail ingestion when metadata is missing

This system intentionally:

- decouples metadata from core persistence
- preserves uploads even when extraction fails
- treats ingestion as an operational pipeline
- allows safe re‑processing and enrichment later

This reflects real‑world ingestion system design.

---

## Failure‑Aware Architecture

Common ingestion failure scenarios include:

- network interruption
- duplicate upload attempts
- partial corruption
- missing or inconsistent metadata
- repeated user retries

The architecture ensures that failures:

- do not corrupt state
- do not create orphan records
- do not expand storage unnecessarily
- can be safely retried

Failures are **events to be handled**, not crashes to recover from.

---

## Extension & Evolution Philosophy

Future enhancements are intentionally predictable:

- background processing queue
- object storage adapter
- ingestion audit dashboard
- caching for common read paths
- RBAC user model
- event stream publishing

All future work builds on a **stable, reliable core** --- no rewrites,
no architectural churn.

---

## Architectural Summary

This project demonstrates:

- disciplined backend platform design
- stability‑first ingestion pipeline
- durable relational modeling
- safety‑first failure handling
- incremental, production‑style evolution

The system is designed so that another engineer can:

- understand it quickly
- reason about behavior
- extend it safely
- trust its persistence model