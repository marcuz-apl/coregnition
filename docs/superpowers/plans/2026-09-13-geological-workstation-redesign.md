# Geological Workstation Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Coregnition into a state-of-the-art geological workstation (split-view continuous well log + high-res core inspector), fix backend test compilation, align `docs/PRD.md`, and establish `AGENTS.md` as the repository engineering standard.

**Architecture:** Modular React 19 + TypeScript frontend with CSS variables for dark/light themes and AAPG SVG patterns; Spring Boot REST backend with SQLite persistence; typed API client with zero minification.

**Tech Stack:** React 19, TypeScript, Vite, Spring Boot 3.4, SQLite, JUnit 5, Mockito, Maven.

**Spec:** docs/superpowers/specs/2026-09-13-geological-workstation-redesign.md

## Global Constraints

- Backend must compile and pass tests cleanly via `./mvnw test`.
- Frontend must build cleanly via `npm --prefix web run build`.
- Zero minified/squashed source files in `web/` or anywhere in the repository.
- Support dual themes: Dark Workstation (default) and Light Technical.
- Preserve all existing backend endpoints and domain logic.

---

### Task 1: Fix Backend Test Compilation & Ensure 100% Green Tests

**Files:**
- Modify: `backend/src/test/java/io/github/marcuzapl/coregnition/backend/workflow/ProjectServiceTest.java:88-95`

**Interfaces:**
- Consumes: `ProjectService.exportCsv(String, boolean)` throws `IOException`
- Produces: Clean JUnit 5 test suite without compilation or runtime errors

- [ ] **Step 1: Check the failing test in ProjectServiceTest**

Inspect `backend/src/test/java/io/github/marcuzapl/coregnition/backend/workflow/ProjectServiceTest.java:88` where `reviewedExportRequiresAtLeastOneInterval()` lacks `throws Exception`.

- [ ] **Step 2: Add throws Exception to the test method signature**

Update `backend/src/test/java/io/github/marcuzapl/coregnition/backend/workflow/ProjectServiceTest.java`:
```java
    @Test
    void reviewedExportRequiresAtLeastOneInterval() throws Exception {
        var project = service.createProject("Empty review");
        assertThrows(ReviewIncompleteException.class, () -> service.exportCsv(project.id(), true));
        assertTrue(service.exportCsv(project.id()).startsWith("segment_id,"));
    }
```

- [ ] **Step 3: Run ./mvnw test to verify all tests pass**

Run: `./mvnw test`
Expected: BUILD SUCCESS with 0 errors and 0 failures.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/io/github/marcuzapl/coregnition/backend/workflow/ProjectServiceTest.java
git commit -m "fix: resolve unhandled exception in ProjectServiceTest"
```

---

### Task 2: Create AGENTS.md Workspace Rules

**Files:**
- Create: `AGENTS.md`

**Interfaces:**
- Consumes: Project engineering constraints, testing procedures, and architecture rules
- Produces: Persistent repository rules loaded automatically by AI coding agents

- [ ] **Step 1: Write AGENTS.md with strict verification and quality standards**

Include:
- Strict verification before completion claims (`./mvnw test` and `npm --prefix web run build`).
- Code craftsmanship: No squashed or minified source code; modular component files.
- UI/UX standards: Professional scientific workstation aesthetics (dark/light themes, AAPG symbology, responsive panes).
- Project architecture breakdown (Spring Boot backend, JavaFX desktop, React web).
- Exact commands for running, building, testing, and packaging.

- [ ] **Step 2: Verify AGENTS.md file presence and format**

Run: `test -f AGENTS.md && wc -l AGENTS.md`
Expected: PASS with complete guidelines.

- [ ] **Step 3: Commit**

```bash
git add AGENTS.md
git commit -m "docs: establish AGENTS.md engineering standards and workspace rules"
```

---

### Task 3: Update docs/PRD.md to Align with Workstation Paradigm

**Files:**
- Modify: `docs/PRD.md`

**Interfaces:**
- Consumes: Approved geological workstation redesign spec
- Produces: Updated Product Requirements Document reflecting split-view workstation, continuous depth track, high-res canvas, and AAPG symbology

- [ ] **Step 1: Update Section 1, 3, and 4 of docs/PRD.md**

Update MVP scope, workflow steps, and acceptance criteria (FR-03, FR-10) to specify:
- Continuous well-log depth track with synchronized core imagery and AAPG lithology symbology.
- High-resolution core inspector with pan/zoom/rotation/region tools.
- Dual theme support (Dark Workstation / Light Technical).

- [ ] **Step 2: Verify docs/PRD.md integrity and diff**

Run: `git diff docs/PRD.md`
Expected: Clean additions describing the workstation architecture.

- [ ] **Step 3: Commit**

```bash
git add docs/PRD.md
git commit -m "docs: update PRD to specify geological workstation architecture"
```

---

### Task 4: Types, REST API Client, and Workspace State Context

**Files:**
- Create: `web/src/types/coregnition.ts`
- Create: `web/src/api/client.ts`
- Create: `web/src/context/WorkspaceContext.tsx`

**Interfaces:**
- Produces:
  - Domain types: `Project`, `Asset`, `Segment`, `Annotation`, `Region`, `Theme`, `ViewMode`
  - API functions: `fetchProjects()`, `createProject()`, `importAsset()`, `createSegment()`, `createAnnotation()`, `undoAnnotation()`, `exportCsvUrl()`, `exportArchiveUrl()`
  - Context hook: `useWorkspace()`

- [ ] **Step 1: Create web/src/types/coregnition.ts**

Define all project domain models and UI state types.

- [ ] **Step 2: Create web/src/api/client.ts**

Implement clean, typed async fetch functions interacting with `http://localhost:3041` (fallback to `/api` or window origin).

- [ ] **Step 3: Create web/src/context/WorkspaceContext.tsx**

Implement `WorkspaceProvider` managing active project, assets, segments, annotations, active selection, view mode (`split`, `log`, `inspector`), and theme (`dark`, `light`).

- [ ] **Step 4: Verify TypeScript compilation**

Run: `npm --prefix web run build`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/types/coregnition.ts web/src/api/client.ts web/src/context/WorkspaceContext.tsx
git commit -m "feat(web): add domain types, api client, and workspace context"
```

---

### Task 5: Design Tokens, CSS Variables & AAPG Lithology Symbology

**Files:**
- Create: `web/src/styles/variables.css`
- Create: `web/src/components/log/LithologyPatterns.tsx`
- Create: `web/src/components/common/Badge.tsx`

**Interfaces:**
- Produces:
  - CSS tokens: `--bg-base`, `--bg-surface`, `--border-color`, `--text-primary`, `--accent-emerald`, `--accent-cyan` for dark/light themes
  - SVG Patterns: `<LithologyPatterns />` supporting `limestone`, `dolostone`, `shale`, `sandstone`, `mixed`
  - Reusable component: `<Badge variant="..." />`

- [ ] **Step 1: Create web/src/styles/variables.css**

Define design tokens for both `[data-theme="dark"]` and `[data-theme="light"]`, plus common typography and shadow scales.

- [ ] **Step 2: Create web/src/components/log/LithologyPatterns.tsx**

Define SVG `<defs>` with pattern elements for masonry brick, rhombic dolomite, shale laminations, sandstone dots, and cross-hatch.

- [ ] **Step 3: Create web/src/components/common/Badge.tsx**

Implement status badge component for `Reviewed`, `Needs Review`, `Uncalibrated`, etc.

- [ ] **Step 4: Verify web build**

Run: `npm --prefix web run build`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/styles/variables.css web/src/components/log/LithologyPatterns.tsx web/src/components/common/Badge.tsx
git commit -m "feat(web): add theme tokens, aapg lithology patterns, and status badges"
```

---

### Task 6: Continuous Well Log Track & Depth Ruler

**Files:**
- Create: `web/src/components/log/DepthRuler.tsx`
- Create: `web/src/components/log/CorePhotoColumn.tsx`
- Create: `web/src/components/log/LithologyTrack.tsx`
- Create: `web/src/components/log/WellLogTrack.tsx`
- Create: `web/src/styles/log.css`

**Interfaces:**
- Consumes: `segments`, `annotations`, `assets`, `activeSelection` from `WorkspaceContext`
- Produces: `<WellLogTrack />` multi-column continuous log visualizer with mouse crosshair tracking

- [ ] **Step 1: Create web/src/components/log/DepthRuler.tsx**

Render vertical depth axis in feet with major/minor tick marks and mouse hover depth guideline.

- [ ] **Step 2: Create web/src/components/log/CorePhotoColumn.tsx**

Render core photo segments aligned to the vertical depth coordinates with orientation indicator.

- [ ] **Step 3: Create web/src/components/log/LithologyTrack.tsx**

Render vertical SVG column filled with AAPG patterns corresponding to each interval with lithology names.

- [ ] **Step 4: Create web/src/components/log/WellLogTrack.tsx and web/src/styles/log.css**

Assemble tracks into a synchronized, scrollable well-log container with track header titles (`DEPTH (FT)`, `CORE PHOTO`, `LITHOLOGY (AAPG)`, `DESCRIPTION`).

- [ ] **Step 5: Verify web build**

Run: `npm --prefix web run build`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add web/src/components/log/ web/src/styles/log.css
git commit -m "feat(web): implement continuous well log track with depth ruler and aapg column"
```

---

### Task 7: High-Resolution Core Inspector Canvas & Image Tools

**Files:**
- Create: `web/src/components/inspector/ImageToolbar.tsx`
- Create: `web/src/components/inspector/CoreInspector.tsx`
- Create: `web/src/styles/inspector.css`

**Interfaces:**
- Consumes: Selected asset, zoom level, rotation, region selection callbacks
- Produces: `<CoreInspector />` high-resolution canvas with pan, zoom (0.25x-8x), rotation, region selection, and enhancement controls

- [ ] **Step 1: Create web/src/components/inspector/ImageToolbar.tsx**

Toolbar with zoom buttons (+, -, fit, 100%), rotation (90° steps), select tool toggle, reset, and contrast/brightness adjustments.

- [ ] **Step 2: Create web/src/components/inspector/CoreInspector.tsx**

Interactive canvas supporting smooth dragging, mouse wheel zoom, coordinate mapping, and region selection box with inverse view transformation.

- [ ] **Step 3: Create web/src/styles/inspector.css**

Styling for canvas, grid backgrounds, floating glassmorphic toolbar, and region overlays.

- [ ] **Step 4: Verify web build**

Run: `npm --prefix web run build`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/inspector/ web/src/styles/inspector.css
git commit -m "feat(web): implement high-resolution core inspector canvas and tools"
```

---

### Task 8: Docks (AssetDock, LithologyDock), HeaderBar & Modals

**Files:**
- Create: `web/src/components/docks/AssetDock.tsx`
- Create: `web/src/components/docks/LithologyDock.tsx`
- Create: `web/src/components/layout/HeaderBar.tsx`
- Create: `web/src/components/common/Modal.tsx`
- Create: `web/src/styles/docks.css`

**Interfaces:**
- Produces:
  - `<HeaderBar />`: Project switcher, view mode switch (`split`, `log`, `inspector`), theme toggle, export menu
  - `<AssetDock />`: Core photo thumbnail list, upload dropzone, depth calibration launcher
  - `<LithologyDock />`: Selected interval editor, one-click lithology presets, review toggle, undo revision
  - `<Modal />`: Accessible dialog for creating projects and calibrating asset depths

- [ ] **Step 1: Create web/src/components/common/Modal.tsx**

Accessible modal with backdrop blur, keyboard ESC closing, and clean styling.

- [ ] **Step 2: Create web/src/components/layout/HeaderBar.tsx**

Header bar with Coregnition branding, project dropdown, view layout switcher, theme toggle, and export buttons (Reviewed CSV, Draft CSV, Portable ZIP).

- [ ] **Step 3: Create web/src/components/docks/AssetDock.tsx and LithologyDock.tsx**

Sidebar docks for core photo assets and fast lithology annotation/review.

- [ ] **Step 4: Verify web build**

Run: `npm --prefix web run build`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/docks/ web/src/components/layout/HeaderBar.tsx web/src/components/common/Modal.tsx web/src/styles/docks.css
git commit -m "feat(web): implement header bar, asset dock, lithology dock, and modal"
```

---

### Task 9: Assemble Split-View Workstation & Full Verification

**Files:**
- Modify: `web/src/App.tsx`
- Modify: `web/src/styles.css`
- Modify: `web/src/main.tsx`

**Interfaces:**
- Consumes: All components from Tasks 4–8
- Produces: Complete, unminified, responsive geological workstation application

- [ ] **Step 1: Replace web/src/App.tsx with clean modular workstation shell**

Integrate `WorkspaceProvider`, `HeaderBar`, `AssetDock`, `WellLogTrack`, `CoreInspector`, and `LithologyDock` with view mode responsiveness (`split`, `log`, `inspector`).

- [ ] **Step 2: Replace web/src/styles.css with clean imports and root workstation styles**

Import `variables.css`, `log.css`, `inspector.css`, and `docks.css`. Ensure no minified/cramped CSS.

- [ ] **Step 3: Run full backend and frontend verification**

Run:
```bash
./mvnw verify
npm --prefix web run build
```
Expected: Both backend Maven verification and frontend Vite build PASS with 0 errors.

- [ ] **Step 4: Commit**

```bash
git add web/src/App.tsx web/src/styles.css web/src/main.tsx
git commit -m "feat(web): assemble geological workstation with split-view and clean architecture"
```
