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

The JavaFX desktop now provides project creation/reopening, image import, zoom/pan/rotation and source-region selection, depth intervals, annotation revision/undo, and CSV/ZIP exchange through the same backend. A Linux application-folder packaging script bundles the Java runtime; native installers and Windows validation remain release gates. There is no trained model or shared deployment yet. Model training remains deferred until a representative labelled dataset and training scope are confirmed.

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

The backend validates supported formats, decoded image content, file size, duplicate checksums, non-negative increasing depths, and non-overlapping intervals on the same image. It also validates configured lithology labels and review states. Annotation writes are revisions; the workspace returns the latest revision for each segment. The client can undo the latest saved annotation, returning to the preceding revision or clearing the first saved label. CSV export includes feet as the declared depth unit, project name as the current well/session identity, the source asset checksum and selected source-region bounds for provenance. Archive import preflights manifest links, decoded image dimensions, content checksums, cumulative uncompressed size, segment bounds, source regions, duplicate checksums, overlap constraints and annotation references before creating a project, so rejected archives do not leave partial records.

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

## Desktop operation and verification

`./scripts/run-local-desktop.sh` builds the JVM modules and launches JavaFX with its own backend on an available loopback port. It uses repository-local `data/`, shared with the local web launcher. Avoid editing the same project in both clients at once; refresh before switching clients.

`./scripts/package-linux-desktop.sh` verifies the modules and builds a Linux application folder and portable tar.gz archive under `desktop/target/linux-package-*/Coregnition`. Launch its `bin/Coregnition` executable. The package includes Java and the backend and stores projects under `~/.local/share/coregnition`. The manual workflow uses Java ImageIO, including TIFF decoding. Native OpenCV bundling remains part of the later recognition packaging gate.

Desktop controls stay disabled while an API operation is pending to prevent competing edits. Network work, image decoding and export writes run outside the JavaFX application thread. Backend validation errors appear in the status line; multi-image import reports individual failed files and refreshes successfully imported assets. Exports download fully before replacing the destination file.

The desktop HTTP tests exercise JSON escaping, field reordering, backend errors, multipart transfer and empty undo responses. The real-backend workflow test starts an isolated service and verifies import, depth/region persistence, annotation revision/undo, CSV provenance and archive restoration. Run the full build from the repository root so the backend JAR exists before desktop tests.

For the JavaFX integration check on Linux with Xvfb:

```sh
xvfb-run -a ./mvnw -pl desktop test -Dcoregnition.uiTest=true
```

This opens the workspace against the temporary backend, selects a saved interval, saves a changed lithology through the UI, verifies persistence and writes `desktop/target/desktop-smoke.png`.

On 2026-09-09, Maven verification passed 21 tests and the JavaFX integration check passed under Xvfb. The generated application successfully started its bundled backend and shut it down on exit. Physical Linux desktop usability and Windows installer validation remain separate from this automated display check.
