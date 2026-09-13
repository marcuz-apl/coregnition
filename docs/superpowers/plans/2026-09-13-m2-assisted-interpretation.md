# Implementation Plan - Milestone 2 (M2): Machine-Assisted Interpretation & Analysis Jobs

Deliver Milestone 2 (M2) as defined in `docs/PRD.md`, implementing asynchronous background analysis jobs, OpenCV-based color/texture feature extraction and lithology classification, full prediction provenance storage in SQLite, and non-destructive human review/tuning UI in the geological workstation frontend.

## Proposed Changes

### Phase 1: Persistence & Records
- Update `ProjectStore.java` with `jobs` and `predictions` tables.
- Create records: `JobRecord.java`, `PredictionRecord.java`, `JobStatus.java`.
- Add store methods: `createJob`, `updateJobStatus`, `job`, `jobs`, `createPrediction`, `predictions`, `predictionsForSegment`.

### Phase 2: Feature Extraction & Classification Engine
- Create `LithologyClassifier.java` and `OpenCvTextureClassifier.java`.
- Implement OpenCV/Java color and texture heuristics against AAPG taxonomy (`limestone`, `dolostone`, `carbonaceous shale`, `mixed`, `unknown`).
- Implement abstention logic (`is_unknown`).

### Phase 3: Asynchronous Job Execution & Workflow Service
- Create `JobService.java` with thread pool executor.
- Handle job lifecycle, core image cropping, classifier evaluation, and prediction recording.
- Update `ProjectService.java` and `ProjectController.java` with new REST endpoints:
  - `POST /api/v1/projects/{id}/jobs/analyze`
  - `GET /api/v1/projects/{id}/jobs`
  - `GET /api/v1/projects/{id}/jobs/{jobId}`
  - `POST /api/v1/projects/{id}/jobs/{jobId}/cancel`
  - `GET /api/v1/projects/{id}/predictions`
  - `GET /api/v1/projects/{id}/segments/{segmentId}/predictions`
  - `POST /api/v1/projects/{id}/segments/{segmentId}/accept-prediction`

### Phase 4: Automated Verification
- Write unit tests for classifier and job service.
- Verify `./mvnw test` passes with 0 failures / 0 errors.

### Phase 5: Workstation Frontend Integration
- Update `types/coregnition.ts` and `api/client.ts`.
- Integrate AI job execution and progress in `HeaderBar.tsx`.
- Add AI recommendation chips in `WellLogTrack.tsx`.
- Add AI recommendation breakdown and 1-click accept in `LithologyDock.tsx`.
- Verify `npm --prefix web run build`.

### Phase 6: Alfazen Commit & Push
- Commit via `./scripts/alfazen-commit`.
- Synchronize branches.
