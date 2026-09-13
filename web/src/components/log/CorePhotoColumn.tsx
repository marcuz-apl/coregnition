import React from "react";
import { Segment, Asset } from "../../types/coregnition";
import { api } from "../../api/client";

interface CorePhotoColumnProps {
  projectId: string;
  segments: Segment[];
  assets: Asset[];
  minDepth: number;
  pixelsPerFoot: number;
  totalHeight: number;
  activeSegmentId: string | null;
  onSelectSegment: (segment: Segment) => void;
}

export function CorePhotoColumn({
  projectId,
  segments,
  assets,
  minDepth,
  pixelsPerFoot,
  totalHeight,
  activeSegmentId,
  onSelectSegment,
}: CorePhotoColumnProps) {
  const assetMap = new Map(assets.map((a) => [a.id, a]));

  return (
    <div className="photo-column-track" style={{ height: `${totalHeight}px` }}>
      {segments.map((seg) => {
        const top = (seg.startDepthFeet - minDepth) * pixelsPerFoot;
        const height = Math.max(20, (seg.endDepthFeet - seg.startDepthFeet) * pixelsPerFoot);
        const isSelected = seg.id === activeSegmentId;
        const asset = assetMap.get(seg.assetId);

        return (
          <div
            key={seg.id}
            className={`photo-segment-card ${isSelected ? "selected" : ""}`}
            style={{ top: `${top}px`, height: `${height}px` }}
            onClick={() => onSelectSegment(seg)}
            title={`${seg.startDepthFeet} – ${seg.endDepthFeet} ft`}
          >
            {asset ? (
              <img
                src={api.getAssetContentUrl(projectId, asset.id)}
                alt={asset.originalName}
                loading="lazy"
              />
            ) : (
              <div style={{ color: "#888", fontSize: "0.6rem", padding: "4px" }}>
                Asset {seg.assetId.slice(0, 6)}
              </div>
            )}
            <span className="segment-orientation-tag">
              {seg.orientation === "BOTTOM_TO_TOP" ? "↑ B→T" : "↓ T→B"}
            </span>
          </div>
        );
      })}
    </div>
  );
}
