# Coregnition

Coregnition is a local-first geological core-image workspace. The web client is the first usable product; a JavaFX desktop client will follow. The current M1 workflow supports project creation, image import, depth calibration in feet, manual lithology annotation, project reload, CSV export, and portable ZIP export/import.

## Run locally

```sh
./scripts/run-local-web.sh
```

Open [http://localhost:3040](http://localhost:3040). The frontend uses port `3040`; its local Java backend uses port `3041`. Keep the launcher terminal open while testing.

To build and test:

```sh
./mvnw verify
cd web && npm ci && npm run build
```

## Project documents

- [Product requirements](PRD.md)
- [Development technote](docs/development-technote.md)
- [Linux M0 verification](docs/m0-linux-x64-verification.md)
- [Changelog](docs/CHANGELOG.md)

## Architecture

React + TypeScript web client → Java/Spring Boot backend → SQLite metadata and project-relative asset files. OpenCV Java bindings are reserved for image processing and future validated model inference. Linux x64 is the current development target; Windows 11 x64 follows the Linux pilot.

Training and recognition are deferred until a representative labelled dataset is available. Current work focuses on the traceable manual workflow and shared backend contract.

## Repository rules

Local databases, imported project assets, web dependencies and supplied image contents are ignored by Git. Do not add proprietary photographs or datasets to source control. Use the tracked Alfazen hooks and `scripts/alfazen-commit` for semantic changes.
