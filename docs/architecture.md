# Architecture

## Purpose

This project is being built as a concurrency-first download engine.

The architecture is designed to make execution flow visible and explainable, not hidden behind a single service method.

## Core Ideas

The engine revolves around these concepts:

- `Job`: one download session started by a user or benchmark
- `Task`: one concrete download unit inside a job
- `Executor`: the async worker pool that runs tasks
- `Tracker`: the in-memory runtime state used to observe progress
- `Downloader`: the component that performs HTTP download and file write

## Current Flow

1. A request starts a job.
2. The job registers multiple tasks.
3. Each task is submitted through Spring async execution.
4. The executor runs tasks using a thread pool.
5. Each task updates runtime status as it moves through `QUEUED`, `RUNNING`, `SUCCESS`, or `FAILED`.
6. Status endpoints expose both job-level and pool-level state.

## Current Components

### Web layer

- `DownloadController`
- `PoolStatusController`

Responsibilities:

- start jobs
- expose job status
- expose task status
- expose pool status

### Application layer

- `DownloadJobService`

Responsibilities:

- orchestrate a download job
- register tasks
- dispatch download execution
- provide read access to job snapshots

### Infrastructure layer

- `AsyncConfig`
- `ImageDownloaderService`
- `DownloadJobTracker`

Responsibilities:

- configure async executor
- execute HTTP download and file write
- hold runtime state for jobs and tasks

## Design Choice: In-memory First

The current tracker is intentionally in-memory.

Why:

- fast feedback during learning
- simple debugging
- easier profiling
- fewer moving parts while concurrency behavior is still being explored

This will likely evolve later into a repository abstraction with persistent storage.

## Design Choice: Explicit Observability

Observability is treated as part of the architecture, not a debugging afterthought.

That is why the system already exposes:

- job status
- task status
- pool status
- throughput-related information

The long-term goal is to add proper metrics export and benchmark reports.

## Near-term Refactoring Direction

The package structure will move toward:

```text
app/
domain/
infra/
web/
```

This will make the codebase easier to scale as jobs, task policies, and source adapters grow.
