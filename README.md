# Coregnition

Coregnition is a planned web and Java desktop application for geological core-image inspection and machine-assisted lithology description in the oil and gas industry. The local web app is the first usable product; the Java desktop app follows with feature parity. Geoscientists will map photographs to depth, review suggested lithologies and export traceable interpretations.

**Current status: M1 local web manual workflow in development.** The Java backend, SQLite persistence, image import, depth calibration, annotation API, CSV export and first React client are available. There is no trained model or installer yet; recognition accuracy has not been established.

## Project documents

- [Initiative](Initiative_coregnition.md): original objectives and technology preferences.
- [Product requirements](PRD.md): proposed scope, architecture, acceptance criteria, data validation and delivery milestones.

## Selected technology

| Component | Recommendation | Rationale |
|---|---|---|
| Desktop | Java + JavaFX | Honors the explicit Java desktop requirement. |
| Web | React + TypeScript | Interactive image inspection and structured interval editing. |
| Shared backend | Java + Spring Boot | One application API for both clients and one image-analysis implementation. |
| Image processing | Official OpenCV Java bindings | Preprocessing and feature extraction; a trained geological model must be developed and validated separately. |
| Local storage | SQLite + asset directory | Simple workstation deployment with metadata in SQLite and original images in files. |
| Shared storage, later | PostgreSQL when justified | Supports the planned move to concurrent, hosted project editing. |

The project owner selected option 2: JavaFX desktop + React web + Java backend, prioritizing Java alignment. OpenCV provides Java bindings for image processing and a DNN API for supported model inference. Evaluate the selected model with OpenCV DNN first; consider ONNX Runtime Java only when compatibility or benchmarks justify another dependency. See the [OpenCV Java DNN documentation](https://docs.opencv.org/4.13.0/javadoc/org/opencv/dnn/Dnn.html) and [ONNX Runtime Java guide](https://onnxruntime.ai/docs/get-started/with-java.html).

A geological classifier still needs representative labelled data and validation. Training can use separate research tooling, potentially Python, while the shipped application runs its backend and inference through Java. Model export compatibility and prediction parity must be verified.

JavaFX is a Java client platform; see its [official guide](https://openjfx.io/openjfx-docs/). SQLite fits local application storage but allows only one writer at a time; see [SQLite usage guidance](https://www.sqlite.org/whentouse.html). These facts inform the recommendation; they are not Coregnition benchmark results.

## Planned user workflow

1. Create a project and well, then import core photographs.
2. Mark core segments, orientation and depth bounds.
3. Annotate lithology manually or request model suggestions when a validated model is available.
4. Review, correct and approve each interval.
5. Export a depth-indexed CSV or portable project archive.

Predictions remain separate from expert interpretations. Unknown, mixed and unassessable intervals are explicit. Sedimentary facies recognition is a future research capability.

## Deployment and project files

The desktop edition is intended to bundle a local Java Spring Boot backend managed by JavaFX, together with a Java runtime and OpenCV native libraries for the target OS and CPU architecture. The web client uses the same API. The first desktop target is Linux x64; Windows 11 x64 follows the Linux pilot. Depth is recorded in feet. Initial web use is local; shared use follows successful functional testing and access-control, concurrency and restore validation.

A saved project will contain SQLite metadata, original images, derived assets and a versioned manifest. A database file alone will not contain the full project. Portable archives must use a consistent database snapshot and include referenced assets. Cross-device synchronization is outside the initial scope.

## Supplied core images

The project owner confirms rights to use these files in `data/core-images/`.

| File | Width × height (pixels) | File size | Format |
|---|---|---|---|
| Supplied core image A | 882 × 1595 | 2,136,395 bytes (2.04 MiB) | PNG, 8-bit RGBA |
| Supplied core image B | 890 × 1611 | 2,234,750 bytes (2.13 MiB) | PNG, 8-bit RGBA |

PNG headers, chunk checksums and decompressed scanline structure were verified. See PRD section 9 for confirmed decisions and remaining inputs.

## Development starting point

M0 is complete for Linux x64. M1 now provides the local web import-to-export workflow against the shared Java backend. Follow with M3 for JavaFX feature parity and offline startup. Windows 11 x64 packaging follows the Linux desktop pilot. Shared deployment follows successful local functional testing and deployment-readiness checks.

Model training is deferred until the project owner supplies a larger dataset and confirms the training scope. The two current images support import/viewer development; they are not an established training or evaluation dataset.

## Local development

Build and test the Java services with `./mvnw verify`. Build the local web client with `cd web && npm ci && npm run build`. Start the backend with `./mvnw -pl backend spring-boot:run`, then start the browser client in another terminal with `cd web && npm run dev`. The browser client expects the backend at `http://127.0.0.1:8787`.

M1 endpoints create projects, import supported images, serve an imported asset, create feet-calibrated segments, append annotation revisions and export CSV. The default local database is `data/coregnition.db`; project assets are stored below `data/projects/`. Both are ignored by Git. Keep proprietary photographs and datasets out of source control unless explicitly cleared for that use.

## Working approach

The initial documents apply the locally available `alfazen-coding` bundle's brainstorming, minimal-engineering (`ponytail`) and evidence-before-completion guidance. Favor a small modular backend, explicit acceptance criteria and representative validation over speculative infrastructure.

## Versioning

The documentation baseline is tagged `v0.0.1`. `VERSION` stores the Alfazen connected identifier (`v0.0.1+YYMMDDc`), and commit subjects carry the same prefix. This milestone contains no runnable application.

After cloning, activate the tracked hooks:

```sh
git config core.hooksPath .githooks
```

Use the tracked commit command for changes that affect SemVer:

```sh
scripts/alfazen-commit 'feat: describe the user-facing capability'
```

`feat:` advances the minor version; `fix:` and `perf:` advance the patch. Major changes require explicit owner approval before setting `ALFAZEN_MAJOR_APPROVED=1`. Direct `git commit` remains suitable for documentation and maintenance commits: the active pre-commit hook advances only the UTC daily build counter and the message hook stamps the same identifier. The command is needed because Git message hooks run too late to stage a semantic version change into the same commit reliably.

`data/core-images/.gitignore` preserves the sample directory while excluding its local contents from Git.
