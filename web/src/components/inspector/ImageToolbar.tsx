import React, { useState } from "react";
import { useWorkspace } from "../../context/WorkspaceContext";

interface ImageToolbarProps {
  isSelecting: boolean;
  onToggleSelect: () => void;
  onReset: () => void;
  onFitWidth: () => void;
  onFitHeight: () => void;
}

export function ImageToolbar({
  isSelecting,
  onToggleSelect,
  onReset,
  onFitWidth,
  onFitHeight,
}: ImageToolbarProps) {
  const {
    zoom,
    setZoom,
    rotation,
    setRotation,
    brightness,
    setBrightness,
    contrast,
    setContrast,
  } = useWorkspace();

  const [showFilters, setShowFilters] = useState(false);

  const zoomIn = () => setZoom((z) => Math.min(8, Math.round((z + 0.25) * 100) / 100));
  const zoomOut = () => setZoom((z) => Math.max(0.25, Math.round((z - 0.25) * 100) / 100));
  const rotateClockwise = () => setRotation((r) => (r + 90) % 360);

  return (
    <>
      <div className="inspector-toolbar">
        <button
          type="button"
          className="toolbar-btn"
          onClick={zoomOut}
          title="Zoom Out (Scroll Down)"
        >
          −
        </button>
        <span className="toolbar-readout">{Math.round(zoom * 100)}%</span>
        <button
          type="button"
          className="toolbar-btn"
          onClick={zoomIn}
          title="Zoom In (Scroll Up)"
        >
          +
        </button>

        <div className="toolbar-divider" />

        <button type="button" className="toolbar-btn" onClick={onFitWidth} title="Fit to Width">
          Fit W
        </button>
        <button type="button" className="toolbar-btn" onClick={onFitHeight} title="Fit to Height">
          Fit H
        </button>
        <button type="button" className="toolbar-btn" onClick={onReset} title="Reset View (100%)">
          1:1
        </button>

        <div className="toolbar-divider" />

        <button
          type="button"
          className="toolbar-btn"
          onClick={rotateClockwise}
          title="Rotate 90° Clockwise"
        >
          ↻ {rotation}°
        </button>

        <button
          type="button"
          className={`toolbar-btn ${isSelecting ? "active" : ""}`}
          onClick={onToggleSelect}
          title="Select Region"
        >
          ⛶ Select
        </button>

        <button
          type="button"
          className={`toolbar-btn ${showFilters ? "active" : ""}`}
          onClick={() => setShowFilters((s) => !s)}
          title="Image Adjustments (Brightness, Contrast)"
        >
          ⚙ Enhance
        </button>
      </div>

      {showFilters && (
        <div className="inspector-enhancement-panel">
          <div className="enhancement-slider-row">
            <label>Brightness</label>
            <input
              type="range"
              min="50"
              max="200"
              value={brightness}
              onChange={(e) => setBrightness(Number(e.target.value))}
            />
            <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.62rem" }}>{brightness}%</span>
          </div>

          <div className="enhancement-slider-row">
            <label>Contrast</label>
            <input
              type="range"
              min="50"
              max="250"
              value={contrast}
              onChange={(e) => setContrast(Number(e.target.value))}
            />
            <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.62rem" }}>{contrast}%</span>
          </div>

          <button
            type="button"
            className="toolbar-btn"
            style={{ width: "100%", marginTop: "4px", height: "24px" }}
            onClick={() => {
              setBrightness(100);
              setContrast(100);
            }}
          >
            Reset Filters
          </button>
        </div>
      )}
    </>
  );
}
