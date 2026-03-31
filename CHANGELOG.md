# Changelog

All notable changes to this project should be documented in this file.

The format is intentionally lightweight for now.

## [Unreleased]

### Added

- job-based download orchestration
- task-level status tracking
- pool status inspection endpoint
- basic project documentation
- issue templates for bug reports and feature requests

### Changed

- project identity moved from a simple multi-threaded downloader toward a concurrency-first download engine
- source code reorganized into `app`, `infra`, and `web` packages

## [v0.1.0-phase-1] - Planned

Phase 1 target:

- engine skeleton
- job/task flow
- basic observability
- package structure cleanup
- initial docs and project roadmap
