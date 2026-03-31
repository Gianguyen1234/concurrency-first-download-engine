# Roadmap

## Phase 1: Engine Skeleton

Goal:

Turn the original downloader demo into a job-oriented engine with basic observability.

Planned / in progress:

- job/task model
- pool status endpoint
- job status endpoint
- task status endpoint
- throughput and latency basics
- simple benchmark entry flow

## Phase 2: Reliability and Control

Goal:

Make the engine resilient and controllable.

Planned:

- retry policy
- timeout policy
- failure classification
- cancel / pause / resume
- persistence for job history
- resumable jobs

## Phase 3: Source Expansion

Goal:

Support more realistic download scenarios.

Planned:

- source adapters
- HTML parsing
- URL extraction
- URL deduplication
- import/export of job input sets

## Phase 4: Observability and Benchmarking

Goal:

Make the engine measurable and comparable.

Planned:

- dashboard
- Prometheus / Micrometer metrics
- Grafana panels
- benchmark reports
- execution mode comparison
- I/O-bound vs CPU-bound comparison scenarios

## Phase 5: Open-source Readiness

Goal:

Make the project comfortable for outside contributors and learners.

Planned:

- plugin system
- examples
- contributor documentation
- architecture decision records
- issue templates refinement
- public roadmap polishing

## Principles

The project should grow in a way that keeps these priorities visible:

- concurrency learning value
- architecture clarity
- observability first
- incremental evolution instead of one large rewrite
