import React, { useState, useRef } from "react";
import { useWorkspace } from "../../context/WorkspaceContext";
import { DepthRuler } from "./DepthRuler";
import { CorePhotoColumn } from "./CorePhotoColumn";
import { LithologyTrack } from "./LithologyTrack";
import { LithologyPatterns } from "./LithologyPatterns";
import { Badge } from "../common/Badge";
import { Segment } from "../../types/coregnition";

const PIXELS_PER_FOOT = 48;

export function WellLogTrack() {
  const {
    activeProject,
    segments,
    assets,
    annotations,
    predictions,
    activeSegment,
    setActiveSegment,
    setActiveAsset,
    acceptPrediction,
  } = useWorkspace();

  const [hoverDepth, setHoverDepth] = useState<number | null>(null);
  const viewportRef = useRef<HTMLDivElement>(null);

  if (!activeProject) {
    return (
      <div className="well-log-container" style={{ justifyContent: "center", alignItems: "center", padding: "40px" }}>
        <p style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>No active project loaded</p>
      </div>
    );
  }

  const assetMap = new Map(assets.map((a) => [a.id, a]));
  const annotationMap = new Map(annotations.map((a) => [a.segmentId, a]));

  // Index latest prediction by segmentId
  const predictionMap = new Map<string, typeof predictions[0]>();
  for (const pred of predictions) {
    if (!predictionMap.has(pred.segmentId)) {
      predictionMap.set(pred.segmentId, pred);
    }
  }

  let minDepth = 0;
  let maxDepth = 20;

  if (segments.length > 0) {
    minDepth = Math.min(...segments.map((s) => s.startDepthFeet));
    maxDepth = Math.max(...segments.map((s) => s.endDepthFeet));
    // round bounds to 5ft marks
    minDepth = Math.floor(minDepth / 5) * 5;
    maxDepth = Math.ceil(maxDepth / 5) * 5;
    if (maxDepth <= minDepth) maxDepth = minDepth + 5;
  }

  const totalFeet = maxDepth - minDepth;
  const totalHeight = Math.max(500, totalFeet * PIXELS_PER_FOOT);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!viewportRef.current) return;
    const rect = viewportRef.current.getBoundingClientRect();
    const scrollTop = viewportRef.current.scrollTop;
    const y = e.clientY - rect.top + scrollTop;
    const depth = minDepth + y / PIXELS_PER_FOOT;
    setHoverDepth(Math.round(depth * 10) / 10);
  };

  const handleMouseLeave = () => setHoverDepth(null);

  const handleSelectSegment = (seg: Segment) => {
    setActiveSegment(seg);
    const asset = assetMap.get(seg.assetId);
    if (asset) setActiveAsset(asset);
  };

  return (
    <div className="well-log-container">
      <LithologyPatterns />
      
      {/* Header Titles */}
      <div className="well-log-header">
        <div className="log-col-depth-header">Depth (ft)</div>
        <div className="log-col-photo-header">Core Photo</div>
        <div className="log-col-lith-header">AAPG Lithology</div>
        <div className="log-col-desc-header">Interval, AI Suggestion & Review Status</div>
      </div>

      {/* Main Continuous Log Viewport */}
      <div
        ref={viewportRef}
        className="well-log-viewport"
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
      >
        <div className="well-log-track-body" style={{ height: `${totalHeight}px` }}>
          {/* Depth Ruler Track */}
          <DepthRuler
            minDepth={minDepth}
            maxDepth={maxDepth}
            pixelsPerFoot={PIXELS_PER_FOOT}
            hoverDepth={hoverDepth}
          />

          {/* Core Photo Column */}
          <CorePhotoColumn
            projectId={activeProject.id}
            segments={segments}
            assets={assets}
            minDepth={minDepth}
            pixelsPerFoot={PIXELS_PER_FOOT}
            totalHeight={totalHeight}
            activeSegmentId={activeSegment?.id || null}
            onSelectSegment={handleSelectSegment}
          />

          {/* Lithology Track */}
          <LithologyTrack
            segments={segments}
            annotations={annotations}
            minDepth={minDepth}
            pixelsPerFoot={PIXELS_PER_FOOT}
            totalHeight={totalHeight}
            activeSegmentId={activeSegment?.id || null}
            onSelectSegment={handleSelectSegment}
          />

          {/* Description & Review Status Column */}
          <div className="description-column-track" style={{ height: `${totalHeight}px` }}>
            {segments.length === 0 ? (
              <div style={{ padding: "30px 15px", color: "var(--text-muted)", fontSize: "0.8rem" }}>
                No core depth intervals calibrated yet. Select an imported image in the left dock to calibrate its interval in feet.
              </div>
            ) : (
              segments.map((seg) => {
                const top = (seg.startDepthFeet - minDepth) * PIXELS_PER_FOOT;
                const height = Math.max(38, (seg.endDepthFeet - seg.startDepthFeet) * PIXELS_PER_FOOT);
                const ann = annotationMap.get(seg.id);
                const pred = predictionMap.get(seg.id);
                const isSelected = seg.id === activeSegment?.id;

                return (
                  <div
                    key={`desc-${seg.id}`}
                    className={`desc-interval-row ${isSelected ? "selected" : ""}`}
                    style={{ top: `${top}px`, height: `${height}px` }}
                    onClick={() => handleSelectSegment(seg)}
                  >
                    <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                      <strong style={{ fontFamily: "var(--font-mono)", color: "var(--text-primary)", fontSize: "0.78rem" }}>
                        {seg.startDepthFeet.toFixed(1)} – {seg.endDepthFeet.toFixed(1)} ft
                      </strong>
                      <span style={{ color: "var(--text-secondary)", textTransform: "capitalize", fontSize: "0.78rem" }}>
                        {ann?.label || "Unassigned"}
                      </span>

                      {/* AI Suggestion Chip */}
                      {pred && (
                        <div
                          style={{
                            display: "inline-flex",
                            alignItems: "center",
                            gap: "4px",
                            padding: "1px 6px",
                            borderRadius: "10px",
                            fontSize: "0.68rem",
                            background: pred.isUnknown
                              ? "rgba(100, 116, 139, 0.15)"
                              : "rgba(56, 189, 248, 0.12)",
                            color: pred.isUnknown ? "var(--text-muted)" : "var(--accent-primary)",
                            border: `1px solid ${pred.isUnknown ? "rgba(100, 116, 139, 0.3)" : "rgba(56, 189, 248, 0.3)"}`,
                          }}
                          title={`AI Model: ${pred.modelId} (${pred.preprocessingVersion})`}
                        >
                          <span>✦</span>
                          <span style={{ textTransform: "capitalize" }}>
                            {pred.isUnknown ? "Unknown (abstained)" : `${pred.suggestedLabel} ${Math.round(pred.confidence * 100)}%`}
                          </span>

                          {/* Quick 1-Click Accept if unreviewed */}
                          {ann?.reviewState !== "REVIEWED" && !pred.isUnknown && (
                            <button
                              type="button"
                              style={{
                                background: "none",
                                border: "none",
                                color: "var(--accent-primary)",
                                cursor: "pointer",
                                padding: "0 2px",
                                fontWeight: "bold",
                                fontSize: "0.75rem",
                              }}
                              onClick={(e) => {
                                e.stopPropagation();
                                acceptPrediction(seg.id, pred.id);
                              }}
                              title={`Accept AI recommendation: ${pred.suggestedLabel}`}
                            >
                              ✓
                            </button>
                          )}
                        </div>
                      )}
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                      {ann?.reviewState === "REVIEWED" ? (
                        <Badge variant="reviewed" size="sm">Reviewed</Badge>
                      ) : (
                        <Badge variant="needs-review" size="sm">Needs Review</Badge>
                      )}
                      {ann && (
                        <span style={{ fontSize: "0.6rem", fontFamily: "var(--font-mono)", color: "var(--text-muted)" }}>
                          rev {ann.revision}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Real-time Hover Crosshair Guideline */}
          {hoverDepth !== null && hoverDepth >= minDepth && hoverDepth <= maxDepth && (
            <div
              className="depth-guideline"
              style={{ top: `${(hoverDepth - minDepth) * PIXELS_PER_FOOT}px` }}
            />
          )}
        </div>
      </div>
    </div>
  );
}
