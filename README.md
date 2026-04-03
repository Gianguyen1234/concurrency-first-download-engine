# Concurrency-first Download Engine

A Spring Boot project for learning concurrency through a real download engine.

This project started as a multi-threaded image downloader and is being evolved into a long-running open-source lab for:

- Java threads and thread pools
- Spring `@Async` and `ThreadPoolTaskExecutor`
- I/O-bound vs CPU-bound behavior
- `CompletableFuture`
- job/task orchestration
- observability, profiling, and benchmarking

## Current State

Phase 1 is completed.

Phase 2 reliability-control is also completed on the current working branch.

What already exists:

- async download execution with `ThreadPoolTaskExecutor`
- job-based download flow
- task-level tracking (`QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELLED`)
- pool inspection endpoint
- throughput and latency metrics
- `baseUrl` validation
- HTTP connect/read timeouts
- failure classification
- retry policy for transient failures
- failure summary per job
- cancel job endpoint
- benchmark mode and benchmark findings

## Vision

The goal is not just to download files.

The goal is to build a concurrency-first download engine where execution strategy, queue behavior, observability, and benchmarking are first-class parts of the system.

This should become a project that is useful for:

- learning Java concurrency deeply
- demonstrating thread-pool behavior under load
- comparing I/O-bound and CPU-bound workloads
- experimenting with job orchestration and monitoring

## API Snapshot

Current endpoints:

- `GET /jobs/start?count=50&baseUrl=https://picsum.photos/300/300`
- `GET /jobs/{jobId}`
- `GET /jobs/{jobId}/tasks`
- `GET /jobs/{jobId}/failure-summary`
- `POST /jobs/{jobId}/cancel`
- `GET /benchmarks/run?count=50&baseUrl=https://picsum.photos/300/300&pollIntervalMs=200&timeoutMillis=60000`
- `GET /pool-status`

## Project Structure

Current source layout:

```text
src/main/java/com/holydev/lab/multithreadediolab
  ConcurrencyFirstDownloadEngineApplication.java
  app/
    DownloadJobService.java
  domain/
    benchmark/
      BenchmarkReport.java
    download/
      FailureType.java
      DownloadResult.java
    job/
      CancelJobResponse.java
      DownloadJobSnapshot.java
      JobFailureSummary.java
      DownloadTaskSnapshot.java
      StartJobResponse.java
      TaskStatus.java
  infra/
    async/
      AsyncConfig.java
    download/
      ImageDownloaderService.java
    tracking/
      DownloadJobTracker.java
  web/
    DownloadController.java
    PoolStatusController.java

docs/
  architecture.md
  roadmap.md
  profiling-guide.md

.github/
  ISSUE_TEMPLATE/
```

## Quick Start

Requirements:

- Java 17+
- Maven Wrapper

Run locally:

```bash
./mvnw spring-boot:run
```

Example flow:

1. Call `GET /jobs/start?count=20`
2. Read the returned `jobId`
3. Call `GET /jobs/{jobId}`
4. Call `GET /jobs/{jobId}/tasks`
5. Call `GET /jobs/{jobId}/failure-summary`
6. Optionally call `POST /jobs/{jobId}/cancel`
7. Call `GET /pool-status`

Example benchmark:

```bash
curl "http://localhost:8080/benchmarks/run?count=50&baseUrl=https://picsum.photos/300/300&pollIntervalMs=200&timeoutMillis=60000"
```

## Why This Project Exists

Small concurrency demos are useful, but they often stop right when the interesting questions begin.

This project is meant to stay around long enough to answer questions like:

- What does a thread pool actually do under pressure?
- Why does I/O-bound work behave differently from CPU-bound work?
- How should async work be observed, measured, and explained?
- How do job orchestration and execution metrics fit together?

## Roadmap

See:

- [docs/architecture.md](docs/architecture.md)
- [docs/flow-overview.vi.md](docs/flow-overview.vi.md)
- [docs/job-snapshot-guide.vi.md](docs/job-snapshot-guide.vi.md)
- [docs/retry-policy.vi.md](docs/retry-policy.vi.md)
- [docs/failure-summary.vi.md](docs/failure-summary.vi.md)
- [docs/cancel-job.vi.md](docs/cancel-job.vi.md)
- [docs/benchmark-mode.vi.md](docs/benchmark-mode.vi.md)
- [docs/benchmark-findings.vi.md](docs/benchmark-findings.vi.md)
- [docs/current-status.vi.md](docs/current-status.vi.md)
- [docs/roadmap.md](docs/roadmap.md)
- [docs/profiling-guide.md](docs/profiling-guide.md)
- [CHANGELOG.md](CHANGELOG.md)

## Git Workflow

This project uses a simple phase-oriented Git strategy.

Branches:

- `main`: relatively stable state
- `phase/*`: larger development stages such as `phase/1-engine-skeleton`
- optional `feat/*`, `refactor/*`, `docs/*`: focused branches created from the current phase branch when needed

Commit style:

- `feat: ...`
- `fix: ...`
- `refactor: ...`
- `docs: ...`
- `test: ...`
- `chore: ...`

Tagging:

- use tags at meaningful project milestones
- current milestone tag: `v0.1.0-phase-1`

The goal is to keep the history understandable as the project evolves from a demo into a long-running open-source engine.

## Contributing

The contributor workflow is still being shaped. For now:

- open an issue for bugs or feature ideas
- keep changes small and focused
- prefer changes that improve architecture clarity, observability, or learning value

Formal contributor guidance will be added in a later phase.
