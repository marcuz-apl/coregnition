# Coregnition Milestone 2 (M2) — Machine-Assisted Interpretation & Analysis Jobs
**Architecture & Design Specification**  
*Date: 2026-09-13*  
*Version: v0.5.0-spec*  
*Standard: Alfazen / PRD FR-06, FR-07, FR-08*

---

## 1. Overview & Objectives

Milestone 2 advances Coregnition from pure manual annotation to **assisted interpretation**. As mandated in `docs/PRD.md`:
1. **FR-06 Analysis jobs**: Persistent background jobs (`queued`, `running`, `succeeded`, `failed`, `cancelled`). Non-blocking execution, crash recovery, and restartability.
2. **FR-07 Prediction provenance**: Rich provenance for all inferences: class scores, abstention status, model identifier, model checksum, preprocessing version, source asset checksum, and segment crop mapping.
3. **FR-08 Expert review**: Predictions never overwrite expert decisions. Geoscientists retain total control to accept, reject, or tune predictions. Reviewed-only export blocks until all intervals have been explicitly reviewed.

---

## 2. Domain & Data Model

### 2.1 Database Schema (SQLite)

```sql
-- Analysis Jobs
CREATE TABLE IF NOT EXISTS jobs (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES projects(id),
    job_type TEXT NOT NULL,
    status TEXT NOT NULL,
    progress REAL NOT NULL,
    error_message TEXT,
    created_at TEXT NOT NULL,
    completed_at TEXT
);

-- Predictions & Provenance
CREATE TABLE IF NOT EXISTS predictions (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES projects(id),
    job_id TEXT NOT NULL REFERENCES jobs(id),
    segment_id TEXT NOT NULL REFERENCES segments(id),
    suggested_label TEXT NOT NULL,
    confidence REAL NOT NULL,
    class_scores TEXT NOT NULL, -- JSON string mapping lithology -> score
    is_unknown INTEGER NOT NULL, -- 0 or 1 (abstention)
    model_id TEXT NOT NULL,
    model_checksum TEXT NOT NULL,
    preprocessing_version TEXT NOT NULL,
    source_asset_checksum TEXT NOT NULL,
    created_at TEXT NOT NULL
);
```

---

## 3. Classifier & Feature Extractor Architecture

### 3.1 Dual-Engine Design
Coregnition implements a dual-mode classifier:
1. **OpenCV Native Engine**: When native OpenCV libraries (`libopencv_java460.so`) are loaded, use OpenCV's native `org.opencv.core.Mat`, color conversion, Sobel/Laplacian gradient computation, and multi-channel histograms.
2. **Pure Java Engine**: If native OpenCV libraries are not available in a particular environment or CI runner, seamlessly fall back to an equivalent pure Java `BufferedImage` pixel-luminance and Sobel gradient kernel.

### 3.2 Geological Lithology Heuristics
Evaluated across AAPG taxonomy:
- **Carbonaceous Shale**: Low luminance ($\mu < 85$), dark grey/black, low saturation, subtle horizontal laminae.
- **Limestone**: High luminance ($\mu > 140$), neutral/cool light grey, fine to bioclastic texture.
- **Dolostone**: Moderate luminance ($90 \le \mu \le 145$), warm tan/buff/brown hues (elevated red/green balance over blue).
- **Mixed**: Bimodal luminance distribution or intermediate texture.
- **Unknown / Unassessable (Abstention)**: If maximum score is below confidence threshold ($< 0.40$) or contrast is near zero (e.g., washouts, core end caps).

---

## 4. Workstation Frontend Integration

1. **Header Bar**: Displays "Run AI Interpretation" action. When active, displays live progress spinner and status badge.
2. **Well Log Track**: Segment bars display machine interpretation chips with confidence tags and 1-click accept buttons.
3. **Lithology Dock**: Provides an in-depth "AI Recommendation & Provenance" panel showing class score bars, model metadata, and instant acceptance controls.
