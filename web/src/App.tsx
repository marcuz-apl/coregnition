import React, { useEffect } from "react";
import { WorkspaceProvider, useWorkspace } from "./context/WorkspaceContext";
import { HeaderBar } from "./components/layout/HeaderBar";
import { AssetDock } from "./components/docks/AssetDock";
import { LithologyDock } from "./components/docks/LithologyDock";
import { WellLogTrack } from "./components/log/WellLogTrack";
import { CoreInspector } from "./components/inspector/CoreInspector";

function WorkstationContent() {
  const {
    viewMode,
    setViewMode,
    isLeftDockOpen,
    toggleLeftDock,
    isRightDockOpen,
    toggleRightDock,
    error,
    clearError,
  } = useWorkspace();

  // Global Keyboard Shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore when typing inside inputs/textareas
      if (["INPUT", "TEXTAREA", "SELECT"].includes((e.target as HTMLElement).tagName)) {
        return;
      }
      if (e.key === "[") {
        e.preventDefault();
        toggleLeftDock();
      } else if (e.key === "]") {
        e.preventDefault();
        toggleRightDock();
      } else if (e.key === "1") {
        e.preventDefault();
        setViewMode("split");
      } else if (e.key === "2") {
        e.preventDefault();
        setViewMode("log");
      } else if (e.key === "3") {
        e.preventDefault();
        setViewMode("inspector");
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [toggleLeftDock, toggleRightDock, setViewMode]);

  return (
    <div className="workstation-app">
      <HeaderBar />

      <main className="workstation-main">
        {/* Left Asset Dock */}
        {isLeftDockOpen && viewMode !== "inspector" && <AssetDock />}

        {/* Center Workspace */}
        <div className="workstation-center">
          {viewMode === "split" && (
            <>
              <div className="pane-log">
                <WellLogTrack />
              </div>
              <div className="pane-inspector">
                <CoreInspector />
              </div>
            </>
          )}

          {viewMode === "log" && (
            <div className="pane-fullscreen">
              <WellLogTrack />
            </div>
          )}

          {viewMode === "inspector" && (
            <div className="pane-fullscreen">
              <CoreInspector />
            </div>
          )}
        </div>

        {/* Right Lithology Dock */}
        {isRightDockOpen && viewMode !== "log" && <LithologyDock />}
      </main>

      {/* Keyboard Shortcuts Hint Bar */}
      <div className="keyboard-hints">
        <span><kbd>[</kbd> Assets</span>
        <span><kbd>]</kbd> Log Panel</span>
        <span><kbd>1</kbd> Split</span>
        <span><kbd>2</kbd> Log</span>
        <span><kbd>3</kbd> Canvas</span>
      </div>

      {/* Error Toast */}
      {error && (
        <div className="error-toast">
          <span>{error}</span>
          <button type="button" className="error-close-btn" onClick={clearError}>
            ×
          </button>
        </div>
      )}
    </div>
  );
}

export function App() {
  return <AppRoot />;
}

function AppRoot() {
  return (
    <WorkspaceProvider>
      <WorkstationContent />
    </WorkspaceProvider>
  );
}

export default App;
