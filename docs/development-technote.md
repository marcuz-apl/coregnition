# Coregnition development technote

This note holds the operational and architectural detail intentionally kept out of the short README.

## Current state

M0 is complete for Linux x64. M1 is the active milestone and provides the local manual workflow:

1. Create or reopen a project.
2. Import one or more PNG, JPEG or TIFF assets.
3. Select an asset and record a depth interval in feet.
4. Record or revise the latest lithology and review state.
5. Reload the project without losing saved records.
6. Export CSV or a ZIP archive containing the workspace manifest and image bytes.
7. Import that archive into a fresh local project, restoring its images, intervals and latest annotations.

There is no trained model, installer or shared deployment yet. Model training remains deferred until a representative labelled dataset and training scope are confirmed.

## Local services

The one-command launcher is:

```sh
./scripts/run-local-web.sh
```

It builds the backend when needed, installs web dependencies when needed, starts both services and stops them on exit.

| Service | Address | Purpose |
|---|---|---|
| React/Vite client | `http://localhost:3040` | Browser interface |
| Spring Boot API | `http://127.0.0.1:3041` | Project and image workflow |
| API health | `http://127.0.0.1:3041/api/v1/health` | Readiness check |

The Vite development server proxies `/api` to port `3041`. If WSL localhost forwarding is unavailable, use the address returned by `hostname -I` with port `3040`.

## Storage

- SQLite metadata: `data/coregnition.db`
- Project asset files: `data/projects/<project-id>/assets/`
- Supplied local image directory: `data/core-images/`

These locations are ignored by Git. A project archive is the portable unit because SQLite metadata references asset files by relative path; a database file alone is incomplete.

The initial local fixtures are PNG files. Their verified dimensions are 882 × 1595 and 890 × 1611 pixels, with file sizes of about 2.04 MiB and 2.13 MiB. They are import/viewer fixtures, not a training or evaluation dataset.

## API surface

- `GET /api/v1/health`
- `GET|POST /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `POST /api/v1/projects/{projectId}/assets`
- `GET /api/v1/projects/{projectId}/assets/{assetId}/content`
- `POST /api/v1/projects/{projectId}/segments`
- `POST /api/v1/projects/{projectId}/segments/{segmentId}/annotations`
- `GET /api/v1/projects/{projectId}/export.csv`
- `GET /api/v1/projects/{projectId}/archive.zip`
- `POST /api/v1/projects/archive`

The backend validates supported formats, decoded image content, file size, duplicate checksums, non-negative increasing depths, and non-overlapping intervals on the same image. It also validates configured lithology labels and review states. Annotation writes are revisions; the workspace returns the latest revision for each segment. The client can undo the latest saved annotation, returning to the preceding revision or clearing the first saved label. Archive import accepts the project manifest and expected asset entries only, rejects unsafe or oversized entries, creates fresh local IDs, and restores the current project records.

The web image viewer operates on the displayed asset without modifying the stored file. It provides zoom, pan, 90-degree rotation, source-region selection and reset controls; selected regions are stored as source-pixel bounds, while the recorded checksum continues to identify the original import.

## Architecture decisions

The selected architecture is JavaFX desktop + React/TypeScript web + Java/Spring Boot backend. Both clients use the shared backend contract. SQLite is appropriate for the single-user local workflow; PostgreSQL remains a later option when concurrent shared editing is justified. OpenCV Java bindings are the planned image-processing path, while model evaluation and training are separate research work.

Depth units are feet. The initial manual vocabulary is limestone, dolostone, carbonaceous shale, unknown, mixed and unassessable. Linux x64 is the current development target; Windows 11 x64 is the next desktop packaging target.

## Versioning

The repository uses Alfazen connected identifiers in `VERSION`. Activate hooks after cloning:

```sh
git config core.hooksPath .githooks
```

Use:

```sh
scripts/alfazen-commit 'feat: describe the user-facing capability'
```

Routine `feat:` and `fix:` commits advance the patch/build within the active milestone line. Only an explicit `milestone:` or `release:` commit advances the minor version. Major changes require explicit owner approval through `ALFAZEN_MAJOR_APPROVED=1`.
