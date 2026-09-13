import React from "react";
import { Segment, Annotation } from "../../types/coregnition";
import { getLithologyPatternId, getLithologyAccentColor } from "./LithologyPatterns";

interface LithologyTrackProps {
  segments: Segment[];
  annotations: Annotation[];
  minDepth: number;
  pixelsPerFoot: number;
  totalHeight: number;
  activeSegmentId: string | null;
  onSelectSegment: (segment: Segment) => void;
}

export function LithologyTrack({
  segments,
  annotations,
  minDepth,
  pixelsPerFoot,
  totalHeight,
  activeSegmentId,
  onSelectSegment,
}: LithologyTrackProps) {
  const annotationMap = new Map(annotations.map((a) => [a.segmentId, a]));

  return (
    <div className="lithology-column-track" style={{ height: `${totalHeight}px` }}>
      <svg
        width="100%"
        height={totalHeight}
        style={{ position: "absolute", top: 0, left: 0, pointerEvents: "none" }}
      >
        {segments.map((seg) => {
          const top = (seg.startDepthFeet - minDepth) * pixelsPerFoot;
          const height = Math.max(20, (seg.endDepthFeet - seg.startDepthFeet) * pixelsPerFoot);
          const ann = annotationMap.get(seg.id);
          const label = ann?.label || "unassigned";
          const patternId = getLithologyPatternId(label);
          const accentColor = getLithologyAccentColor(label);
          const isSelected = seg.id === activeSegmentId;

          return (
            <g key={`svg-${seg.id}`}>
              <rect
                x="6"
                y={top}
                width="128"
                height={height}
                rx="4"
                fill={`url(#${patternId})`}
                stroke={isSelected ? "var(--accent-emerald)" : accentColor}
                strokeWidth={isSelected ? "2" : "1"}
                strokeOpacity={isSelected ? 1 : 0.6}
              />
            </g>
          );
        })}
      </svg>

      {segments.map((seg) => {
        const top = (seg.startDepthFeet - minDepth) * pixelsPerFoot;
        const height = Math.max(20, (seg.endDepthFeet - seg.startDepthFeet) * pixelsPerFoot);
        const ann = annotationMap.get(seg.id);
        const label = ann?.label || "unassigned";
        const isSelected = seg.id === activeSegmentId;

        return (
          <div
            key={seg.id}
            className={`lithology-segment-block ${isSelected ? "selected" : ""}`}
            style={{ top: `${top}px`, height: `${height}px`, background: "transparent" }}
            onClick={() => onSelectSegment(seg)}
            title={`${seg.startDepthFeet} – ${seg.endDepthFeet} ft: ${label}`}
          >
            <span className="lithology-label-overlay">
              {label}
            </span>
          </div>
        );
      })}
    </div>
  );
}
