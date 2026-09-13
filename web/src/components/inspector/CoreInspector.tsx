import React, { useState, useRef, useEffect } from "react";
import { useWorkspace } from "../../context/WorkspaceContext";
import { ImageToolbar } from "./ImageToolbar";
import { api } from "../../api/client";
import { Region } from "../../types/coregnition";

export function CoreInspector() {
  const {
    activeProject,
    activeAsset,
    activeSegment,
    activeRegion,
    setActiveRegion,
    zoom,
    setZoom,
    rotation,
    setRotation,
    brightness,
    contrast,
  } = useWorkspace();

  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [isSelecting, setIsSelecting] = useState(false);
  const [draftRegion, setDraftRegion] = useState<{ x: number; y: number; width: number; height: number } | null>(null);

  const viewportRef = useRef<HTMLDivElement>(null);
  const imgRef = useRef<HTMLImageElement>(null);

  // Sync segment region when active segment changes
  useEffect(() => {
    if (activeSegment && activeSegment.regionWidth && activeSegment.regionHeight) {
      setActiveRegion({
        x: activeSegment.regionX || 0,
        y: activeSegment.regionY || 0,
        width: activeSegment.regionWidth,
        height: activeSegment.regionHeight,
      });
    } else {
      setActiveRegion(null);
    }
  }, [activeSegment, setActiveRegion]);

  const resetView = () => {
    setZoom(1);
    setRotation(0);
    setOffset({ x: 0, y: 0 });
    setDraftRegion(null);
  };

  const fitWidth = () => {
    if (!viewportRef.current || !activeAsset) return;
    const vp = viewportRef.current.getBoundingClientRect();
    const targetZoom = Math.max(0.2, (vp.width - 60) / activeAsset.width);
    setZoom(Math.round(targetZoom * 100) / 100);
    setOffset({ x: 0, y: 0 });
  };

  const fitHeight = () => {
    if (!viewportRef.current || !activeAsset) return;
    const vp = viewportRef.current.getBoundingClientRect();
    const targetZoom = Math.max(0.2, (vp.height - 60) / activeAsset.height);
    setZoom(Math.round(targetZoom * 100) / 100);
    setOffset({ x: 0, y: 0 });
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY < 0 ? 0.15 : -0.15;
    setZoom((z) => Math.min(8, Math.max(0.25, Math.round((z + delta) * 100) / 100)));
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!activeAsset) return;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);

    if (isSelecting) {
      const rect = e.currentTarget.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const clickY = e.clientY - rect.top;
      setDragStart({ x: clickX, y: clickY });
      setDraftRegion({ x: clickX, y: clickY, width: 0, height: 0 });
    } else {
      setIsDragging(true);
      setDragStart({ x: e.clientX - offset.x, y: e.clientY - offset.y });
    }
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isSelecting && draftRegion) {
      const rect = e.currentTarget.getBoundingClientRect();
      const currentX = e.clientX - rect.left;
      const currentY = e.clientY - rect.top;

      setDraftRegion({
        x: Math.min(dragStart.x, currentX),
        y: Math.min(dragStart.y, currentY),
        width: Math.abs(currentX - dragStart.x),
        height: Math.abs(currentY - dragStart.y),
      });
    } else if (isDragging) {
      setOffset({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y,
      });
    }
  };

  const handlePointerUp = () => {
    if (isSelecting && draftRegion && draftRegion.width > 5 && draftRegion.height > 5 && imgRef.current && activeAsset) {
      const imgRect = imgRef.current.getBoundingClientRect();
      const relX = Math.max(0, draftRegion.x - (imgRect.left - (viewportRef.current?.getBoundingClientRect().left || 0)));
      const relY = Math.max(0, draftRegion.y - (imgRect.top - (viewportRef.current?.getBoundingClientRect().top || 0)));

      const scaleX = activeAsset.width / imgRect.width;
      const scaleY = activeAsset.height / imgRect.height;

      const sourceRegion: Region = {
        x: Math.round(relX * scaleX),
        y: Math.round(relY * scaleY),
        width: Math.max(1, Math.round(draftRegion.width * scaleX)),
        height: Math.max(1, Math.round(draftRegion.height * scaleY)),
      };

      setActiveRegion(sourceRegion);
      setIsSelecting(false);
      setDraftRegion(null);
    } else if (isSelecting) {
      setDraftRegion(null);
    }
    setIsDragging(false);
  };

  if (!activeProject || !activeAsset) {
    return (
      <div className="core-inspector-container" style={{ justifyContent: "center", alignItems: "center" }}>
        <p style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
          Select a core photograph to inspect high-resolution imagery.
        </p>
      </div>
    );
  }

  const assetUrl = api.getAssetContentUrl(activeProject.id, activeAsset.id);

  return (
    <div className="core-inspector-container">
      <ImageToolbar
        isSelecting={isSelecting}
        onToggleSelect={() => {
          setIsSelecting((s) => !s);
          setDraftRegion(null);
        }}
        onReset={resetView}
        onFitWidth={fitWidth}
        onFitHeight={fitHeight}
      />

      <div
        ref={viewportRef}
        className={`inspector-viewport ${isDragging ? "grabbing" : ""} ${isSelecting ? "select-mode" : ""}`}
        onWheel={handleWheel}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
      >
        <div
          className="inspector-image-wrapper"
          style={{
            transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom}) rotate(${rotation}deg)`,
            filter: `brightness(${brightness}%) contrast(${contrast}%)`,
          }}
        >
          <img
            ref={imgRef}
            src={assetUrl}
            alt={activeAsset.originalName}
            draggable={false}
          />

          {/* Calibrated / Selected Region Overlay */}
          {activeRegion && (
            <div
              className="inspector-region-overlay"
              style={{
                left: `${(activeRegion.x / activeAsset.width) * 100}%`,
                top: `${(activeRegion.y / activeAsset.height) * 100}%`,
                width: `${(activeRegion.width / activeAsset.width) * 100}%`,
                height: `${(activeRegion.height / activeAsset.height) * 100}%`,
              }}
              title={`Source Region: (${activeRegion.x}, ${activeRegion.y}) ${activeRegion.width}x${activeRegion.height}px`}
            />
          )}
        </div>

        {/* Dynamic Draft Region during user mouse-drag */}
        {draftRegion && (
          <div
            className="inspector-region-overlay draft"
            style={{
              left: `${draftRegion.x}px`,
              top: `${draftRegion.y}px`,
              width: `${draftRegion.width}px`,
              height: `${draftRegion.height}px`,
            }}
          />
        )}
      </div>
    </div>
  );
}
