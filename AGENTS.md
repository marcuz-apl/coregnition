# Coregnition — Agent Guidelines & Engineering Standards

This document defines the strict engineering standards, architecture, and workflows for all AI coding agents and human contributors working on the Coregnition repository.

---

## 1. Core Operating Principles

1. **Strict Verification Before Claims (Evidence Before Assertions)**:
   - Before claiming any task is complete or committing changes, you **MUST** run automated verification commands and verify they pass:
     - Backend & Desktop: `./mvnw test` (must return BUILD SUCCESS with 0 errors and 0 failures)
     - Frontend: `npm --prefix web run build` (must compile TypeScript and bundle with Vite with 0 errors)
   - Zero tolerance for committing broken test suites or failing builds.
2. **Code Craftsmanship & Readability**:
   - **Never commit minified, compressed, or single-line source code** (e.g., in `web/src/App.tsx` or `styles.css`).
   - Write clean, modular, properly indented code. Separate large files into focused components with single responsibilities.
   - Use strict TypeScript types and avoid `any`.
3. **Scientific Workstation UI/UX Standards**:
   - The frontend is a professional geological workstation (Petrel / WellCAD paradigm).
   - Avoid cramped multi-step form wizards with tiny preview thumbnails.
   - Maintain the split-view architecture: continuous vertical well-log track (depth ruler, photo column, AAPG lithology symbols) alongside an expansive high-resolution core inspector canvas (smooth pan, zoom, rotation, region mapping).
   - Maintain dual themes: Dark Workstation (default) and Light Technical.
4. **Autonomous & Decisive Execution**:
   - Act decisively. Don't enter repetitive stalled loops or ask trivial questions when the spec and requirements are established.

---

## 2. Architecture Map

```
coregnition/
├── backend/            # Java 21 / Spring Boot 3.4 REST API & persistence
│   ├── src/main/java/  # Controllers, services, and SQLite store (port 3041)
│   └── src/test/java/  # Comprehensive JUnit 5 & Mockito test suites
├── desktop/            # JavaFX 21 desktop client & launcher
│   └── src/main/java/  # DesktopWorkspace, CoreImageViewer, BackendApi
├── web/                # React 19 / TypeScript / Vite frontend (port 3040)
│   ├── src/components/ # Modular workstation components (layout, log, inspector, docks)
│   ├── src/context/    # WorkspaceContext state management
│   ├── src/api/        # Clean typed REST client
│   └── src/styles/     # Design tokens and modular stylesheets
├── docs/               # Technical documentation, PRD, specs, and plans
├── scripts/            # Launchers, packaging, and commit utilities
└── pom.xml             # Root Maven configuration
```

---

## 3. Standard Commands

### Running Locally
- **Web client + Backend**:
  ```sh
  ./scripts/run-local-web.sh
  ```
  Web UI: [http://localhost:3040](http://localhost:3040) | Local Backend API: [http://localhost:3041](http://localhost:3041)
- **Desktop client**:
  ```sh
  ./scripts/run-local-desktop.sh
  ```

### Testing & Verification
- **Full Backend & Desktop Tests**:
  ```sh
  ./mvnw test
  ```
- **Frontend Build Verification**:
  ```sh
  npm --prefix web run build
  ```
- **Full Verification Suite**:
  ```sh
  ./mvnw verify && npm --prefix web run build
  ```

### Packaging
- **Linux Desktop Application Bundle**:
  ```sh
  ./scripts/package-linux-desktop.sh
  ```

---

## 4. Git & Commit Protocol

- Follow the project's Alfazen versioning standard.
- Commit messages follow the conventional format (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`).
- When committing, the repository's tracked hooks automatically prepend the version signature (`v0.X.Y+YYMMDD<seq>`).
