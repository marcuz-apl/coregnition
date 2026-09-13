import React from "react";

export function LithologyPatterns() {
  return (
    <svg style={{ position: "absolute", width: 0, height: 0, overflow: "hidden" }} aria-hidden="true">
      <defs>
        {/* Limestone - AAPG standard brick pattern */}
        <pattern id="pattern-limestone" width="24" height="12" patternUnits="userSpaceOnUse">
          <rect width="24" height="12" fill="#3b82f6" fillOpacity="0.25" />
          <path d="M 0 0 L 24 0 M 0 6 L 24 6 M 0 12 L 24 12 M 0 0 L 0 6 M 12 6 L 12 12 M 24 0 L 24 6" stroke="#60a5fa" strokeWidth="1" strokeOpacity="0.8" />
        </pattern>

        {/* Dolostone - AAPG slanted rhombic brick pattern */}
        <pattern id="pattern-dolostone" width="24" height="12" patternUnits="userSpaceOnUse">
          <rect width="24" height="12" fill="#06b6d4" fillOpacity="0.25" />
          <path d="M 0 0 L 24 0 M 0 6 L 24 6 M 0 12 L 24 12 M 2 0 L 0 6 M 14 6 L 12 12 M 14 0 L 12 6 M 26 6 L 24 12" stroke="#2dd4bf" strokeWidth="1" strokeOpacity="0.8" />
        </pattern>

        {/* Carbonaceous Shale - AAPG parallel laminated line pattern */}
        <pattern id="pattern-shale" width="20" height="8" patternUnits="userSpaceOnUse">
          <rect width="20" height="8" fill="#475569" fillOpacity="0.35" />
          <line x1="0" y1="2" x2="20" y2="2" stroke="#94a3b8" strokeWidth="1" strokeOpacity="0.8" />
          <line x1="0" y1="6" x2="20" y2="6" stroke="#94a3b8" strokeWidth="1" strokeDasharray="3 3" strokeOpacity="0.6" />
        </pattern>

        {/* Sandstone - AAPG stippled dot matrix */}
        <pattern id="pattern-sandstone" width="16" height="16" patternUnits="userSpaceOnUse">
          <rect width="16" height="16" fill="#eab308" fillOpacity="0.22" />
          <circle cx="4" cy="4" r="1" fill="#facc15" />
          <circle cx="12" cy="4" r="1" fill="#facc15" />
          <circle cx="8" cy="10" r="1.2" fill="#facc15" />
          <circle cx="4" cy="14" r="1" fill="#facc15" />
          <circle cx="14" cy="14" r="1" fill="#facc15" />
        </pattern>

        {/* Mixed - Diagonal cross-hatch */}
        <pattern id="pattern-mixed" width="12" height="12" patternUnits="userSpaceOnUse">
          <rect width="12" height="12" fill="#8b5cf6" fillOpacity="0.25" />
          <path d="M 0 0 L 12 12 M 12 0 L 0 12" stroke="#a78bfa" strokeWidth="1" strokeOpacity="0.7" />
        </pattern>

        {/* Unknown - Simple diagonal hatch */}
        <pattern id="pattern-unknown" width="10" height="10" patternUnits="userSpaceOnUse">
          <rect width="10" height="10" fill="#64748b" fillOpacity="0.15" />
          <path d="M 0 10 L 10 0" stroke="#94a3b8" strokeWidth="1" strokeOpacity="0.4" strokeDasharray="2 2" />
        </pattern>
      </defs>
    </svg>
  );
}

export function getLithologyPatternId(label: string): string {
  const norm = label.toLowerCase();
  if (norm.includes("lime")) return "pattern-limestone";
  if (norm.includes("dolo")) return "pattern-dolostone";
  if (norm.includes("shale")) return "pattern-shale";
  if (norm.includes("sand")) return "pattern-sandstone";
  if (norm.includes("mix")) return "pattern-mixed";
  return "pattern-unknown";
}

export function getLithologyAccentColor(label: string): string {
  const norm = label.toLowerCase();
  if (norm.includes("lime")) return "#3b82f6";
  if (norm.includes("dolo")) return "#06b6d4";
  if (norm.includes("shale")) return "#64748b";
  if (norm.includes("sand")) return "#eab308";
  if (norm.includes("mix")) return "#8b5cf6";
  return "#94a3b8";
}
