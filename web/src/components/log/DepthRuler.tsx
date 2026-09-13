import React from "react";

interface DepthRulerProps {
  minDepth: number;
  maxDepth: number;
  pixelsPerFoot: number;
  hoverDepth: number | null;
}

export function DepthRuler({
  minDepth,
  maxDepth,
  pixelsPerFoot,
  hoverDepth,
}: DepthRulerProps) {
  const startInt = Math.floor(minDepth);
  const endInt = Math.ceil(maxDepth);
  const totalFeet = Math.max(1, endInt - startInt);
  const totalHeight = totalFeet * pixelsPerFoot;

  const ticks: React.ReactNode[] = [];

  for (let d = startInt; d <= endInt; d++) {
    const top = (d - startInt) * pixelsPerFoot;
    const isMajor = d % 5 === 0;

    if (isMajor) {
      ticks.push(
        <div key={`maj-${d}`} className="depth-tick-major" style={{ top: `${top}px` }}>
          <span>{d}</span>
        </div>
      );
    } else {
      ticks.push(
        <div key={`min-${d}`} className="depth-tick-minor" style={{ top: `${top}px` }} />
      );
    }
  }

  return (
    <div className="depth-ruler-track" style={{ height: `${totalHeight}px` }}>
      {ticks}
      {hoverDepth !== null && hoverDepth >= minDepth && hoverDepth <= maxDepth && (
        <div
          className="depth-guideline-badge"
          style={{ top: `${(hoverDepth - startInt) * pixelsPerFoot - 8}px` }}
        >
          {hoverDepth.toFixed(1)} ft
        </div>
      )}
    </div>
  );
}
