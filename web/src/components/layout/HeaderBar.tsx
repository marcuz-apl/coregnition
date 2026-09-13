import React, { useState } from "react";
import { useWorkspace } from "../../context/WorkspaceContext";
import { Modal } from "../common/Modal";
import { Badge } from "../common/Badge";
import { api } from "../../api/client";

export function HeaderBar() {
  const {
    projects,
    activeProject,
    segments,
    annotations,
    selectProject,
    createProject,
    viewMode,
    setViewMode,
    theme,
    toggleTheme,
    isLeftDockOpen,
    toggleLeftDock,
    isRightDockOpen,
    toggleRightDock,
  } = useWorkspace();

  const [isNewProjOpen, setIsNewProjOpen] = useState(false);
  const [newProjName, setNewProjName] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  // Calculate review progress
  const totalSegments = segments.length;
  const reviewedCount = annotations.filter((a) => a.reviewState === "REVIEWED").length;
  const isAllReviewed = totalSegments > 0 && reviewedCount === totalSegments;

  const handleCreateProject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newProjName.trim()) return;
    try {
      setIsCreating(true);
      await createProject(newProjName.trim());
      setNewProjName("");
      setIsNewProjOpen(false);
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <header className="header-bar">
      {/* Left section: Logo, Version & Project Switcher */}
      <div className="header-left">
        <div className="brand-logo">
          <span className="brand-logo-icon">C</span>
          <span>Coregnition</span>
          <Badge variant="neutral" size="sm">v0.4.0</Badge>
        </div>

        <button
          type="button"
          className={`toolbar-btn ${isLeftDockOpen ? "active" : ""}`}
          onClick={toggleLeftDock}
          title="Toggle Asset Dock"
        >
          ☰ Assets
        </button>

        {/* Project Selector */}
        <div className="project-selector-dropdown">
          <select
            className="project-select"
            value={activeProject?.id || ""}
            onChange={(e) => selectProject(e.target.value)}
          >
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>

          <button
            type="button"
            className="header-btn"
            style={{ padding: "0 8px", height: "32px" }}
            onClick={() => setIsNewProjOpen(true)}
            title="Create New Project"
          >
            + New
          </button>
        </div>
      </div>

      {/* Center section: View Mode Toggles */}
      <div className="header-center">
        <div className="view-mode-pill">
          <button
            type="button"
            className={`view-mode-btn ${viewMode === "split" ? "active" : ""}`}
            onClick={() => setViewMode("split")}
          >
            Split View
          </button>
          <button
            type="button"
            className={`view-mode-btn ${viewMode === "log" ? "active" : ""}`}
            onClick={() => setViewMode("log")}
          >
            Log View
          </button>
          <button
            type="button"
            className={`view-mode-btn ${viewMode === "inspector" ? "active" : ""}`}
            onClick={() => setViewMode("inspector")}
          >
            Inspector
          </button>
        </div>
      </div>

      {/* Right section: Review Progress, Exports & Theme Switcher */}
      <div className="header-right">
        {activeProject && totalSegments > 0 && (
          <Badge variant={isAllReviewed ? "reviewed" : "needs-review"} size="md">
            {reviewedCount}/{totalSegments} Reviewed
          </Badge>
        )}

        {activeProject && (
          <>
            <a
              className="header-btn"
              href={api.getExportCsvUrl(activeProject.id, true)}
              title="Export Reviewed Intervals CSV"
            >
              Reviewed CSV ↓
            </a>
            <a
              className="header-btn"
              href={api.getExportCsvUrl(activeProject.id, false)}
              title="Export Full CSV (including draft intervals)"
            >
              Draft CSV ↓
            </a>
            <a
              className="header-btn"
              href={api.getExportArchiveUrl(activeProject.id)}
              title="Export Portable Project ZIP Archive"
            >
              ZIP Archive ↓
            </a>
          </>
        )}

        <button
          type="button"
          className="header-btn"
          onClick={toggleTheme}
          title={`Switch to ${theme === "dark" ? "Light" : "Dark"} Theme`}
        >
          {theme === "dark" ? "☀ Light" : "☾ Dark"}
        </button>

        <button
          type="button"
          className={`toolbar-btn ${isRightDockOpen ? "active" : ""}`}
          onClick={toggleRightDock}
          title="Toggle Lithology Inspector Dock"
        >
          ✎ Log Panel
        </button>
      </div>

      {/* New Project Modal */}
      <Modal
        isOpen={isNewProjOpen}
        onClose={() => setIsNewProjOpen(false)}
        title="Create Geological Project"
        footer={
          <>
            <button
              type="button"
              className="header-btn"
              onClick={() => setIsNewProjOpen(false)}
            >
              Cancel
            </button>
            <button
              type="button"
              className="header-btn primary"
              onClick={handleCreateProject}
              disabled={!newProjName.trim() || isCreating}
            >
              {isCreating ? "Creating…" : "Create Project"}
            </button>
          </>
        }
      >
        <form onSubmit={handleCreateProject} style={{ display: "grid", gap: "10px" }}>
          <label style={{ fontSize: "0.78rem", fontWeight: 600, color: "var(--text-secondary)" }}>
            Project & Well Name
          </label>
          <input
            type="text"
            className="input-text"
            placeholder="e.g. Well Bravo-04 Core Section"
            value={newProjName}
            onChange={(e) => setNewProjName(e.target.value)}
            autoFocus
          />
          <p style={{ fontSize: "0.68rem", color: "var(--text-muted)", margin: 0 }}>
            Creates a local-first workspace for photograph inspection and depth-indexed manual lithology logging.
          </p>
        </form>
      </Modal>
    </header>
  );
}
