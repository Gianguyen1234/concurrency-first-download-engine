# Profiling Guide

## What to Look For

This project is meant to teach the difference between CPU-bound and I/O-bound execution.

When profiling, always ask:

- Is the CPU actually busy?
- Or are threads mostly waiting on network or file I/O?

## IntelliJ Profiler

Useful views:

- `Call Tree`
- `Timeline`
- `Events`

Useful modes:

- `CPU Time`: shows time spent actively using CPU
- `All Time`: shows total elapsed time, including waiting
- `Allocation`: useful for memory analysis, not the main view for this project

## Reading I/O-bound Behavior

Typical signs:

- task durations vary a lot
- total elapsed time is large
- CPU time is much smaller than elapsed time
- threads spend long periods waiting

In this project, slow downloads usually mean:

- network wait
- remote server latency
- file I/O delay

Not heavy CPU computation.

## Reading CPU-bound Behavior

Typical signs:

- CPU usage stays high
- task durations are driven by computation
- CPU time is close to elapsed time
- more cores usually help more directly

## Practical Workflow

1. Start a download job.
2. Observe `GET /jobs/{jobId}` and `GET /pool-status`.
3. Profile the run in IntelliJ.
4. Compare `CPU Time` and `All Time`.
5. Check whether the pool is busy, queued, or mostly waiting.

## Metrics That Matter

At the job level:

- total requested
- completed
- success / failure counts
- total wall time
- average task latency
- throughput

At the pool level:

- pool size
- active count
- queue size
- completed task count

## Why This Matters

Concurrency is easy to romanticize and hard to reason about.

This project is intended to make that behavior concrete:

- what work is running
- what work is queued
- what work is blocked on I/O
- what changes when the environment changes
