import React, { useState, useRef } from "react";
import { useWorkspace } from "../../context/WorkspaceContext";
import { Modal } from "../common/Modal";
import { Badge } from "../common/Badge";
import { api } from "../../api/client";

export function AssetDock() {
  const {
    activeProject,
    assets,
    segments,
    activeAsset,
    setActiveAsset,
    importAsset,
    createSegment,
    importArchive,
    activeRegion,
  } = useWorkspace();

  const [isCalibrateOpen, setIsCalibrateOpen] = useState(false);
  const [startDepth, setStartDepth] = useState("");
  const [endDepth, setEndDepth] = useState("");
  const [orientation, setOrientation] = useState("TOP_TO_BOTTOM");
  const [isSavingSegment, setIsSavingSegment] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const archiveInputRef = useRef<HTMLInputElement>(null);

  if (!activeProject) return null;

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await importAsset(file);
    } finally {
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleArchiveUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await importArchive(file);
    } finally {
      if (archiveInputRef.current) archiveInputRef.current.value = "";
    }
  };

  const handleSaveSegment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeAsset || !startDepth || !endDepth) return;
    const start = parseFloat(startDepth);
    const end = parseFloat(endDepth);
    if (isNaN(start) || isNaN(end) || start >= end) {
      alert("Please specify valid depth bounds where Start Depth < End Depth.");
      return;
    }

    try {
      setIsSavingSegment(true);
      await createSegment(activeAsset.id, start, end, orientation, activeRegion);
      setIsCalibrateOpen(false);
      setStartDepth("");
      setEndDepth("");
    } finally {
      setIsSavingSegment(false);
    }
  };

  return (
    <aside className="dock-panel dock-left">
      <div className="dock-header">
        <span>Core Assets ({assets.length})</span>
        <button
          type="button"
          className="toolbar-btn"
          style={{ height: "24px", fontSize: "0.64rem" }}
          onClick={() => archiveInputRef.current?.click()}
          title="Import Portable Project Archive ZIP"
        >
          Restore ZIP
        </button>
      </div>

      <div className="dock-body">
        {/* Drag-and-drop Import Dropzone */}
        <div className="dropzone-box" onClick={() => fileInputRef.current?.click()}>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/png,image/jpeg,image/tiff"
            style={{ display: "none" }}
            onChange={handleFileUpload}
          />
          <input
            ref={archiveInputRef}
            type="file"
            accept=".zip,application/zip"
            style={{ display: "none" }}
            onChange={handleArchiveUpload}
          />
          <div className="brand-logo-icon" style={{ width: "26px", height: "26px", fontSize: "0.85rem" }}>
            ↑
          </div>
          <span className="dropzone-title">Import Core Photo</span>
          <span className="dropzone-subtitle">PNG, JPEG, or TIFF (feet)</span>
        </div>

        {/* Core Photo List */}
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {assets.map((asset) => {
            const isSelected = activeAsset?.id === asset.id;
            const assetSegments = segments.filter((s) => s.assetId === asset.id);

            return (
              <div
                key={asset.id}
                className={`asset-card ${isSelected ? "selected" : ""}`}
                onClick={() => setActiveAsset(asset)}
              >
                <div className="asset-card-thumb">
                  <img
                    src={api.getAssetContentUrl(activeProject.id, asset.id)}
                    alt={asset.originalName}
                  />
                </div>
                <div className="asset-card-info">
                  <span className="asset-card-name" title={asset.originalName}>
                    {asset.originalName}
                  </span>
                  <span className="asset-card-dims">
                    {asset.width} × {asset.height} px
                  </span>
                  <div style={{ display: "flex", gap: "4px", marginTop: "2px" }}>
                    {assetSegments.length > 0 ? (
                      <Badge variant="reviewed" size="sm">
                        {assetSegments.length} Interval{assetSegments.length > 1 ? "s" : ""}
                      </Badge>
                    ) : (
                      <Badge variant="needs-review" size="sm">
                        Uncalibrated
                      </Badge>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Depth Calibration Action */}
        {activeAsset && (
          <div style={{ marginTop: "auto", paddingTop: "12px", borderTop: "1px solid var(--border-color)" }}>
            <button
              type="button"
              className="header-btn primary"
              style={{ width: "100%", justifyContent: "center" }}
              onClick={() => setIsCalibrateOpen(true)}
            >
              + Calibrate Depth Interval
            </button>
          </div>
        )}
      </div>

      {/* Depth Calibration Modal */}
      <Modal
        isOpen={isCalibrateOpen}
        onClose={() => setIsCalibrateOpen(false)}
        title="Calibrate Core Depth Interval"
        footer={
          <>
            <button type="button" className="header-btn" onClick={() => setIsCalibrateOpen(false)}>
              Cancel
            </button>
            <button
              type="button"
              className="header-btn primary"
              onClick={handleSaveSegment}
              disabled={isSavingSegment || !startDepth || !endDepth}
            >
              {isSavingSegment ? "Saving…" : "Save Interval"}
            </button>
          </>
        }
      >
        <form onSubmit={handleSaveSegment} style={{ display: "grid", gap: "12px" }}>
          <p style={{ fontSize: "0.74rem", color: "var(--text-secondary)", margin: 0 }}>
            Assign the measured wellbore depth in <strong>feet</strong> to <strong>{activeAsset?.originalName}</strong>:
          </p>

          <div className="field-row">
            <div className="field-group">
              <label>Start Depth (ft)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className="input-text"
                placeholder="e.g. 8420.0"
                value={startDepth}
                onChange={(e) => setStartDepth(e.target.value)}
                autoFocus
              />
            </div>
            <div className="field-group">
              <label>End Depth (ft)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className="input-text"
                placeholder="e.g. 8423.5"
                value={endDepth}
                onChange={(e) => setEndDepth(e.target.value)}
              />
            </div>
          </div>

          <div className="field-group">
            <label>Photograph Orientation</label>
            <select
              className="input-text"
              value={orientation}
              onChange={(e) => setOrientation(e.target.value)}
            >
              <option value="TOP_TO_BOTTOM">Top to bottom (normal depth increase)</option>
              <option value="BOTTOM_TO_TOP">Bottom to top (reversed orientation)</option>
            </select>
          </div>

          {activeRegion && (
            <div style={{ padding: "8px 10px", background: "var(--bg-surface-elevated)", borderRadius: "6px", fontSize: "0.68rem" }}>
              <span style={{ color: "var(--accent-emerald)", fontWeight: 700 }}>✓ Region Attached: </span>
              <span style={{ fontFamily: "var(--font-mono)" }}>
                {activeRegion.width}×{activeRegion.height}px at ({activeRegion.x}, {activeRegion.y})
              </span>
            </div>
          )}
        </form>
      </Modal>
    </aside>
  );
}
