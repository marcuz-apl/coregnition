import React, { useState, useEffect } from "react";
import { useWorkspace } from "../../context/WorkspaceContext";
import { AAPG_LITHOLOGIES } from "../../types/coregnition";
import { getLithologyAccentColor } from "../log/LithologyPatterns";
import { Badge } from "../common/Badge";

export function LithologyDock() {
  const {
    activeSegment,
    annotations,
    predictions,
    createAnnotation,
    undoAnnotation,
    acceptPrediction,
    runAnalysis,
    activeJob,
    isLoading,
  } = useWorkspace();

  const [selectedLabel, setSelectedLabel] = useState<string>("limestone");
  const [reviewState, setReviewState] = useState<string>("REVIEWED");
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Sync with current annotation of active segment
  const currentAnnotation = annotations.find((a) => a.segmentId === activeSegment?.id);

  // Sync with current prediction of active segment
  const currentPrediction = predictions.find((p) => p.segmentId === activeSegment?.id);

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

  const handleAcceptAI = async () => {
    if (!activeSegment || !currentPrediction) return;
    await acceptPrediction(activeSegment.id, currentPrediction.id);
  };

  const thickness = (activeSegment.endDepthFeet - activeSegment.startDepthFeet).toFixed(2);

  // Parse class scores if JSON string
  let parsedScores: Record<string, number> = {};
  if (currentPrediction) {
    if (typeof currentPrediction.classScores === "string") {
      try {
        parsedScores = JSON.parse(currentPrediction.classScores);
      } catch {
        parsedScores = {};
      }
    } else if (typeof currentPrediction.classScores === "object") {
      parsedScores = currentPrediction.classScores;
    }
  }

  const isJobRunning = activeJob && (activeJob.status === "QUEUED" || activeJob.status === "RUNNING");

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

        {/* M2: Machine Interpretation & Provenance Panel */}
        <div
          style={{
            padding: "12px",
            background: "linear-gradient(180deg, rgba(56, 189, 248, 0.05) 0%, var(--bg-surface-elevated) 100%)",
            borderRadius: "var(--radius-md)",
            border: "1px solid rgba(56, 189, 248, 0.25)",
            display: "flex",
            flexDirection: "column",
            gap: "10px",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: "0.78rem", fontWeight: 700, color: "var(--accent-primary)", display: "flex", alignItems: "center", gap: "6px" }}>
              <span>✦</span> AI Interpretation
            </span>
            <button
              type="button"
              className="header-btn"
              style={{ fontSize: "0.65rem", padding: "1px 6px", height: "22px" }}
              onClick={() => runAnalysis(activeSegment.id)}
              disabled={isJobRunning || isLoading}
            >
              {isJobRunning ? "Analyzing…" : "Re-analyze"}
            </button>
          </div>

          {currentPrediction ? (
            <>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
                <div>
                  <div style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}>Suggested Lithology</div>
                  <strong style={{ fontSize: "0.95rem", color: "var(--text-primary)", textTransform: "capitalize" }}>
                    {currentPrediction.isUnknown ? "Unknown (Abstained)" : currentPrediction.suggestedLabel}
                  </strong>
                </div>
                <div style={{ textAlign: "right" }}>
                  <div style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}>Confidence</div>
                  <strong style={{ fontFamily: "var(--font-mono)", color: "var(--accent-primary)", fontSize: "0.95rem" }}>
                    {Math.round(currentPrediction.confidence * 100)}%
                  </strong>
                </div>
              </div>

              {/* Confidence Progress Bar */}
              <div style={{ width: "100%", height: "4px", background: "var(--bg-secondary)", borderRadius: "2px", overflow: "hidden" }}>
                <div
                  style={{
                    width: `${Math.min(100, Math.round(currentPrediction.confidence * 100))}%`,
                    height: "100%",
                    background: currentPrediction.isUnknown
                      ? "var(--text-muted)"
                      : "linear-gradient(90deg, var(--accent-primary), var(--accent-purple))",
                  }}
                />
              </div>

              {/* Class Probabilities Distribution */}
              {Object.keys(parsedScores).length > 0 && (
                <div style={{ display: "grid", gap: "4px", marginTop: "4px" }}>
                  <span style={{ fontSize: "0.68rem", fontWeight: 600, color: "var(--text-muted)" }}>
                    Class Score Breakdown
                  </span>
                  {Object.entries(parsedScores).map(([cls, score]) => (
                    <div key={cls} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", fontSize: "0.65rem" }}>
                      <span style={{ textTransform: "capitalize", color: "var(--text-secondary)" }}>{cls}</span>
                      <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                        <div style={{ width: "60px", height: "3px", background: "var(--bg-secondary)", borderRadius: "2px", overflow: "hidden" }}>
                          <div style={{ width: `${Math.round(score * 100)}%`, height: "100%", background: getLithologyAccentColor(cls) }} />
                        </div>
                        <span style={{ fontFamily: "var(--font-mono)", width: "26px", textAlign: "right", color: "var(--text-muted)" }}>
                          {Math.round(score * 100)}%
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Provenance Audit Footer */}
              <div style={{ fontSize: "0.6rem", color: "var(--text-muted)", borderTop: "1px dashed var(--border-color)", paddingTop: "6px", display: "grid", gap: "2px" }}>
                <div>Model: <code>{currentPrediction.modelId}</code></div>
                <div>Checksum: <code>{currentPrediction.modelChecksum.slice(0, 20)}…</code></div>
                <div>Pipeline: <code>{currentPrediction.preprocessingVersion}</code></div>
              </div>

              {/* Accept AI Button */}
              {!currentPrediction.isUnknown && (
                <button
                  type="button"
                  className="header-btn"
                  style={{
                    width: "100%",
                    justifyContent: "center",
                    borderColor: "var(--accent-primary)",
                    color: "var(--accent-primary)",
                    fontWeight: 700,
                  }}
                  onClick={handleAcceptAI}
                  disabled={isLoading}
                >
                  ✓ Accept Recommendation ({currentPrediction.suggestedLabel})
                </button>
              )}
            </>
          ) : (
            <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", textAlign: "center", padding: "8px 0" }}>
              No AI prediction generated yet for this interval.
              <button
                type="button"
                className="header-btn"
                style={{ width: "100%", marginTop: "8px", justifyContent: "center" }}
                onClick={() => runAnalysis(activeSegment.id)}
                disabled={isJobRunning || isLoading}
              >
                ✦ Run AI on this interval
              </button>
            </div>
          )}
        </div>

        {/* Lithology Form */}
        <form onSubmit={handleSaveDescription} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          {/* AAPG Presets */}
          <div className="form-section">
            <span className="form-section-title">Manual AAPG Lithology Override</span>
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
