# Review and Platform Readiness

**Goal:** Close the reviewed-export and source-region correctness gaps in the manual workflow, and prepare native Windows builds.
**Spec:** PRD.md FR-03, FR-08, FR-10 and the confirmed Windows 11 x64 packaging target.
**Design:** The shared backend validates the same row snapshot it exports. Draft export remains available; reviewed export requires at least one interval and every latest annotation reviewed. Both clients expose review progress and handle server validation. Image selections map through inverse view transforms into source pixels. Windows packaging uses the existing JavaFX/backend architecture, platform-local data and a bundled Java runtime.

- [ ] Add service and HTTP regression tests for missing/unreviewed annotations, successful review, and undo reopening review.
- [ ] Implement reviewed-only CSV with explicit conflict errors and preserve draft export.
- [ ] Add web and desktop reviewed export controls and review progress; verify both clients.
- [ ] Correct web source-region mapping after zoom, pan and rotation with geometry regression tests.
- [ ] Add Windows launch/package scripts, platform data-path tests and Windows CI artifact builds.
- [ ] Verify Maven, web build, geometry tests, JavaFX UI, and available platform scripts; document Windows execution status accurately.
