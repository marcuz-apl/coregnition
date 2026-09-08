# Changelog

Coregnition follows the Alfazen connected version format `v<major>.<minor>.<patch>+<YYMMDDc>`.

Routine feature and fix commits advance the patch/build within the active milestone line. Minor versions are reserved for explicit `milestone:` or `release:` commits.

## Semantic version progression

```text
v0.0.1 (Planning and architecture baseline)
  │
  └─► v0.1.0 (Local Java foundation)
        │
        └─► v0.1.1 (Versioning reliability and Linux OpenCV validation)
              │
              └─► v0.3.7+260908k (M1 local web manual workflow)
```

## Milestone Change Log Details

### [v0.3.7+260908k] — 2026-09-08
**feat: M1 local web manual workflow foundation**
- **Type**: `feat` / `web-first rollout` / `milestone-level`
- **Scope**: Java Spring Boot backend, SQLite persistence, project asset storage, image import, depth segments, annotation revisions, CSV export.
- **Key Deliverables**:
  - **Persistence and workflow contract**: Adds project, asset, segment and annotation records in a local SQLite database, with feet as the depth unit and the confirmed limestone, dolostone and carbonaceous-shale vocabulary plus review states.
  - **Safe image handling**: Restricts imports to PNG, JPEG and TIFF, validates decoded images, stores project-relative assets, records SHA-256 checksums and rejects duplicate imports.
  - **Client rollout**: Defines the local web app as the first usable M1 client; JavaFX desktop parity follows in M3 against the same API.
  - **Usable first pass**: Adds a guided project workspace with progress rail, image preview and metadata, depth validation, clear loading/error states, reset workflow and responsive styling.
  - **Reloadable review state**: Project workspaces now restore imported images, saved intervals and each interval's latest lithology/review annotation.
  - **Portable export**: Adds a self-contained ZIP archive download with the workspace manifest and imported image bytes alongside the existing CSV export.
  - **Portable restore**: Imports a project ZIP into a new local project, restoring image assets, calibrated intervals and latest annotation state.
  - **Interval integrity**: Prevents overlapping depth intervals on the same image before they can create an ambiguous export.
  - **Annotation revisions**: Adds an explicit undo action that restores the prior saved description or clears the initial one.
  - **Image inspection**: Replaces the static preview with zoom, pan, rotation and reset controls while preserving the original imported asset.
  - **Source regions**: Persists selected image regions as validated source-pixel bounds on calibrated segments and restores them with the project.
  - **Export provenance**: Extends CSV rows with the feet unit, current project/well identity, source asset checksum and selected region bounds.
  - **Archive integrity**: Preflights archive content and cross-record references before creating a restored project, preventing partial imports from malformed archives.
  - **Archive interval safety**: Rejects non-finite, overlapping or duplicate-checksum records before restoration begins.
  - **Archive checksum integrity**: Verifies each archived image byte stream against its manifest SHA-256 before restoration.
  - **Archive bomb protection**: Caps cumulative decompressed ZIP content at the same 300 MB safety limit as the upload.
  - **Documentation structure**: Keeps the README as a concise entry point and moves operational details into `docs/development-technote.md`.

### [v0.1.1] — 2026-09-08
**docs: Linux foundation and release tooling**
- **Type**: `docs|fix` / `release-foundation`
- **Scope**: Alfazen versioning hooks, Maven wrapper, M0 verification documentation.
- **Key Deliverables**:
  - **Native validation**: Ubuntu OpenCV Java 4.6.0 loaded and decoded both local PNG fixtures on Linux x64.
  - **Reliability**: Semantic version changes are staged through `scripts/alfazen-commit` so `VERSION` and commit prefixes remain aligned.

### [v0.1.0] — 2026-09-08
**feat: local Java foundation**
- **Type**: `feat` / `architecture-foundation`
- **Scope**: Spring Boot backend, JavaFX launcher, Maven build.
- **Key Deliverables**:
  - **Runtime**: Loopback health endpoint, packaged backend JAR and JavaFX launcher with managed backend process support.
  - **Image groundwork**: PNG metadata inspection and automated backend tests.

### [v0.0.1] — 2026-09-08
**docs: Coregnition planning baseline**
- **Type**: `docs` / `planning-baseline`
- **Scope**: PRD, README, Linux-first rollout decision and local project conventions.
- **Key Deliverables**:
  - **Product definition**: Architecture, milestones, acceptance criteria, storage decision and deferred model-training track.
