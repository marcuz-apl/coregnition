import React, { useState, useEffect } from "react";
import { useWorkspace } from "../../context/WorkspaceContext";
import { AAPG_LITHOLOGIES } from "../../types/coregnition";
import { getLithologyAccentColor } from "../log/LithologyPatterns";
import { Badge } from "../common/Badge";

export function LithologyDock() {
  const {
    activeSegment,
    annotations,
    createAnnotation,
    undoAnnotation,
    isLoading,
  } = useWorkspace();

  const [selectedLabel, setSelectedLabel] = useState<string>("limestone");
  const [reviewState, setReviewState] = useState<string>("REVIEWED");
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Sync with current annotation of active segment
  const currentAnnotation = annotations.find((a) => a.segmentId === activeSegment?.id);

  useEffect(() => {
    if (currentAnnotation) {
      setSelectedLabel(currentAnnotation.label);
      setReviewState(currentAnnotation.reviewState);
    } else {
      setSelectedLabel("limestone");
      setReviewState("REVIEWED");
    }
    setSaveSuccess(false);
  }, [activeSegment, currentAnnotation]);

  if (!activeSegment) {
    return (
      <aside className="dock-panel dock-right">
        <div className="dock-header">
          <span>Lithology Logging</span>
        </div>
        <div className="dock-body" style={{ justifyContent: "center", alignItems: "center" }}>
          <p style={{ color: "var(--text-muted)", fontSize: "0.78rem", textAlign: "center", padding: "20px" }}>
            Click an interval on the <strong>Well Log Track</strong> to inspect or record lithological descriptions.
          </p>
        </div>
      </aside>
    );
  }

  const handleSaveDescription = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeSegment) return;
    try {
      setIsSaving(true);
      await createAnnotation(activeSegment.id, selectedLabel, reviewState);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } finally {
      setIsSaving(false);
    }
  };

  const handleUndo = async () => {
    if (!activeSegment) return;
    await undoAnnotation(activeSegment.id);
  };

  const thickness = (activeSegment.endDepthFeet - activeSegment.startDepthFeet).toFixed(2);

  return (
    <aside className="dock-panel dock-right">
      <div className="dock-header">
        <span>Interval Logging</span>
        {currentAnnotation?.reviewState === "REVIEWED" ? (
          <Badge variant="reviewed" size="sm">Reviewed</Badge>
        ) : (
          <Badge variant="needs-review" size="sm">Needs Review</Badge>
        )}
      </div>

      <div className="dock-body">
        {/* Selected Interval Card */}
        <div style={{ padding: "12px", background: "var(--bg-surface-elevated)", borderRadius: "var(--radius-md)", border: "1px solid var(--border-color)" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
            <strong style={{ fontFamily: "var(--font-mono)", fontSize: "0.9rem", color: "var(--text-primary)" }}>
              {activeSegment.startDepthFeet.toFixed(1)} – {activeSegment.endDepthFeet.toFixed(1)} ft
            </strong>
            <span style={{ fontSize: "0.68rem", color: "var(--text-secondary)", fontFamily: "var(--font-mono)" }}>
              Δ {thickness} ft
            </span>
          </div>
          <div style={{ marginTop: "6px", fontSize: "0.68rem", color: "var(--text-muted)" }}>
            Orientation: {activeSegment.orientation === "BOTTOM_TO_TOP" ? "Bottom to top (reversed)" : "Top to bottom"}
          </div>
        </div>

        {/* Lithology Form */}
        <form onSubmit={handleSaveDescription} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          {/* AAPG Presets */}
          <div className="form-section">
            <span className="form-section-title">AAPG Lithology Classes</span>
            <div className="lithology-grid">
              {AAPG_LITHOLOGIES.map((lith) => {
                const isSelected = selectedLabel.toLowerCase() === lith.toLowerCase();
                const accentColor = getLithologyAccentColor(lith);

                return (
                  <button
                    key={lith}
                    type="button"
                    className={`lithology-preset-btn ${isSelected ? "active" : ""}`}
                    onClick={() => setSelectedLabel(lith)}
                  >
                    <span
                      style={{
                        width: "8px",
                        height: "8px",
                        borderRadius: "50%",
                        backgroundColor: accentColor,
                        flexShrink: 0,
                      }}
                    />
                    {lith}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Review Status Selector */}
          <div className="form-section">
            <span className="form-section-title">Review Sign-Off</span>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                className={`toolbar-btn ${reviewState === "REVIEWED" ? "active" : ""}`}
                style={{ flex: 1, height: "34px" }}
                onClick={() => setReviewState("REVIEWED")}
              >
                ✓ Reviewed
              </button>
              <button
                type="button"
                className={`toolbar-btn ${reviewState === "UNREVIEWED" ? "active" : ""}`}
                style={{ flex: 1, height: "34px" }}
                onClick={() => setReviewState("UNREVIEWED")}
              >
                ? Needs Review
              </button>
            </div>
          </div>

          {/* Action Buttons */}
          <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginTop: "8px" }}>
            <button
              type="submit"
              className="header-btn primary"
              style={{ width: "100%", justifyContent: "center", height: "36px" }}
              disabled={isSaving || isLoading}
            >
              {isSaving ? "Saving…" : "Save Description"}
            </button>

            {currentAnnotation && (
              <button
                type="button"
                className="header-btn"
                style={{ width: "100%", justifyContent: "center" }}
                onClick={handleUndo}
                disabled={isSaving || isLoading}
              >
                Undo Last Save (Rev {currentAnnotation.revision})
              </button>
            )}
          </div>

          {saveSuccess && (
            <div style={{ padding: "8px 10px", background: "var(--accent-emerald-subtle)", border: "1px solid var(--accent-emerald)", borderRadius: "6px", color: "var(--accent-emerald)", fontSize: "0.72rem", textAlign: "center", fontWeight: 700 }}>
              ✓ Description Saved Successfully
            </div>
          )}
        </form>
      </div>
    </aside>
  );
}
