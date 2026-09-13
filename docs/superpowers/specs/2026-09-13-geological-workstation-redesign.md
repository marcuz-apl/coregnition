# Geological Workstation Redesign & Engineering Rigor Spec

**Date:** 2026-09-13  
**Status:** Approved  
**Scope:** Frontend UI/UX Redesign, Backend Test Fix, PRD Alignment, and AGENTS.md Rules  

---

## 1. Overview & Problem Statement

Coregnition is a local-first geological core-image analysis workspace for oil and gas geoscientists. The previous implementation suffered from critical engineering and design shortcomings:
1. **Broken Test Suite**: `ProjectServiceTest.java:89` fails compilation due to an unhandled `IOException` in `reviewedExportRequiresAtLeastOneInterval()`.
2. **Unmaintainable Source Code**: `web/src/App.tsx` and `web/src/styles.css` were compressed into dense single-line code blocks, preventing standard engineering practices.
3. **Mismatched UI/UX**: The frontend was implemented as a cramped 4-step wizard with a tiny 92px viewer, failing the requirements of professional geoscientists who require macro well-log context alongside high-resolution micro photo inspection.

This specification overhauls the frontend into an industry-grade geological workstation (Approach 1: Split-View Workstation), repairs backend test integrity, establishes `AGENTS.md` as the repository operational standard, and aligns `docs/PRD.md`.

---

## 2. Architecture & Workstation Layout

### 2.1 Multi-Pane Split-View Workstation
The web frontend transitions from a sequential wizard to a synchronized multi-pane scientific workstation:

1. **Top Utility Bar (`HeaderBar`)**:
   - Project selector and creation dialog trigger.
   - Well metadata and depth measurement units (feet).
   - Layout mode switcher: `Split View` (default), `Well Log Focus`, `Inspector Focus`.
   - Theme toggle: `Dark Workstation` (default) / `Light Technical`.
   - Review progress counter (`X of Y intervals reviewed`).
   - Export menu: Reviewed-only CSV, Draft CSV, and Portable Project Archive ZIP.

2. **Left Navigation Dock (`AssetDock`, Collapsible)**:
   - Imported core photograph list with thumbnails, filename, resolution, and depth bounds.
   - Drag-and-drop file import dropzone.
   - Depth calibration trigger for uncalibrated images.

3. **Center-Left: Continuous Well Log Track (`WellLogTrack`)**:
   - Continuous vertical depth axis synchronized across all project intervals.
   - **Track 1 - Depth Ruler**: Continuous ruler marked in feet with major and minor tick intervals. Real-time cursor crosshair tracking the mouse position.
   - **Track 2 - Core Photo Strip**: Scaled core segments rendered in true calibrated depth order.
   - **Track 3 - Lithology Column**: Industry-standard AAPG/USGS geological SVG pattern fills with color coding.
   - **Track 4 - Annotation & Review Badges**: Lithology name, interval bounds, and review state indicator (`Reviewed` vs `Needs Review`).

4. **Center-Right: High-Resolution Core Inspector (`CoreInspector`)**:
   - Full-fidelity interactive canvas for deep inspection of individual core photographs.
   - Pan and zoom (0.25× to 8×) with mouse wheel, pinch gesture, and zoom presets (Fit Width, Fit Height, 100%, 200%).
   - View rotation (0°, 90°, 180°, 270° clockwise).
   - Visual enhancement tools: Brightness and contrast sliders for low-light core features.
   - Source-coordinate region selector with precise inverse transformation mapping.

5. **Right Property Dock (`LithologyDock`, Collapsible)**:
   - Active interval metadata: Start/End depth in feet, orientation (`Top to Bottom` / `Bottom to Top`).
   - One-click lithology presets: Limestone, Dolostone, Carbonaceous Shale, Sandstone, Mixed, Unknown.
   - Review status switcher: `Reviewed` (green check) vs `Needs Review`.
   - Reversible action stack: Undo last saved annotation with revision history.

---

## 3. Visual System & Symbology

### 3.1 Themes & Design Tokens
- **Dark Workstation Theme (Default)**:
  - Base: `#0f141a`
  - Docks/Panels: `#161e27`
  - Cards/Elevated: `#1e293b`
  - Borders: `#273549`
  - Accent Emerald: `#00dc82`
  - Selection Cyan: `#38bdf8`
  - Text: Primary `#f1f5f9`, Muted `#94a3b8`
- **Light Technical Theme**:
  - Base: `#f8fafc`
  - Docks/Panels: `#ffffff`
  - Cards/Elevated: `#f1f5f9`
  - Borders: `#e2e8f0`
  - Accent Emerald: `#059669`
  - Selection Cobalt: `#0284c7`
  - Text: Primary `#0f172a`, Muted `#64748b`

### 3.2 AAPG / USGS Geological Symbology
- **Limestone (`limestone`)**: Light Blue (`#60a5fa`) with traditional masonry brick SVG pattern.
- **Dolostone (`dolostone`)**: Cyan/Teal (`#2dd4bf`) with diamond/slanted brick SVG pattern.
- **Carbonaceous Shale (`carbonaceous shale`)**: Charcoal Slate (`#64748b`) with horizontal parallel laminations.
- **Sandstone (`sandstone`)**: Amber/Gold (`#facc15`) with stippled dot matrix pattern.
- **Mixed / Unknown (`mixed`, `unknown`)**: Violet/Neutral (`#a78bfa`) with diagonal cross-hatch.

---

## 4. Component Hierarchy & Module Structure

Replace minified code with a modular, clean TypeScript structure:
- `web/src/types/coregnition.ts`: Strong domain interfaces (`Project`, `Asset`, `Segment`, `Annotation`, `Region`).
- `web/src/api/client.ts`: Typed API client handling all backend REST endpoints and error parsing.
- `web/src/context/WorkspaceContext.tsx`: React Context for state, selection, and view controls.
- `web/src/components/layout/HeaderBar.tsx`: Top application navigation and tools.
- `web/src/components/layout/AppShell.tsx`: Resizable/collapsible multi-pane layout.
- `web/src/components/log/WellLogTrack.tsx`: Multi-track continuous well-log visualizer.
- `web/src/components/log/DepthRuler.tsx`: Depth ruler with hover cursor crosshair.
- `web/src/components/log/CorePhotoColumn.tsx`: Calibrated core photos scaled to depth.
- `web/src/components/log/LithologyTrack.tsx`: Lithology intervals filled with AAPG SVG patterns.
- `web/src/components/log/LithologyPatterns.tsx`: SVG pattern definitions.
- `web/src/components/inspector/CoreInspector.tsx`: High-resolution canvas with pan/zoom/rotate.
- `web/src/components/inspector/ImageToolbar.tsx`: Canvas manipulation toolbars.
- `web/src/components/docks/AssetDock.tsx`: Core photo management dock.
- `web/src/components/docks/LithologyDock.tsx`: Property inspector and annotation editor.
- `web/src/components/common/Badge.tsx`: Reusable status badges.
- `web/src/components/common/Modal.tsx`: Accessible dialogs for calibration and project creation.
- `web/src/styles/variables.css`: Theming tokens and variables.
- `web/src/styles/main.css`: Clean, unminified, responsive CSS layout.

---

## 5. Engineering Standards & Workspace Rules

### 5.1 Backend Test Fix
- Fix `backend/src/test/java/io/github/marcuzapl/coregnition/backend/workflow/ProjectServiceTest.java`: Add `throws Exception` to `reviewedExportRequiresAtLeastOneInterval()`.
- Run `./mvnw test` to ensure 100% test passing across the backend and desktop modules.

### 5.2 Creation of `AGENTS.md`
Establish `AGENTS.md` at repository root defining:
1. **Verification Gate**: Mandatory `./mvnw test` and `npm --prefix web run build` before claiming completion.
2. **Code Cleanliness**: Zero minified/squashed code in source files; standard indentation and typed interfaces.
3. **UI/UX Workstation Standard**: Preserving the scientific workstation paradigm.
4. **Architecture & Commands**: Complete reference of Spring Boot backend, JavaFX desktop, React web, and launch scripts.

### 5.3 Updates to `docs/PRD.md`
Update `docs/PRD.md` Section 3, Section 4 (FR-03, FR-10), and Section 5 to codify the split-view geological workstation and AAPG symbology.
