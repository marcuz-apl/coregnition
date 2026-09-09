# Desktop Manual Workflow Implementation Plan

**Goal:** Complete the JavaFX manual workflow against the existing shared backend.
**Architecture:** Typed HTTP client, JavaFX workspace, source-coordinate image viewer. All HTTP and file work runs off the JavaFX thread; requests are serialized by disabling workspace controls while busy.
**Spec:** PRD.md sections 3–5, scoped to manual description; trained recognition and cross-platform installers remain separate release gates.
**Constraints:** Java 21, JavaFX 21.0.6, feet, preserved originals, shared backend validation and persistence.

- [x] Replace regex JSON parsing with Jackson; test escaped names, reordered fields, errors and request payloads using an HTTP fixture.
- [x] Add project/workspace, multipart import, segment/annotation/undo and binary export API operations; verify against a real temporary backend.
- [x] Build desktop project/asset/interval navigation and editing with explicit busy/error feedback.
- [x] Add zoom/pan/rotation and source-pixel region selection; test coordinate mapping independently.
- [x] Add a desktop launcher and update operating instructions.
- [x] Run Maven verification, web build, real-backend desktop contract smoke test, and JavaFX display smoke test where available.

## Verification result

2026-09-09: Maven verify passed 21 tests (13 backend, 8 desktop); the web production build passed. JavaFX integration under Xvfb saved an annotation through the UI and verified backend persistence. Both the packaged runtime/backend startup and shutdown passed. The development launcher uses direct Java arguments to preserve paths without Maven plugin quote parsing. Review found an archive upload limit mismatch; desktop and servlet limits now match the 300 MB archive contract, with a 110 MB real HTTP regression check. The generated Linux app is a manual-workflow pilot, not a trained recognition or Windows installer release.
