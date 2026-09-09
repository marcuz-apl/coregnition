import { useLayoutEffect, useRef, useState } from "react";
import type { PointerEvent as ReactPointerEvent } from "react";
import { selectionRegion, viewportToSource } from "./imageGeometry";
import type { Point, Region, View } from "./imageGeometry";
export type { Region } from "./imageGeometry";

type Props = { src: string; alt: string; sourceWidth: number; sourceHeight: number; region: Region | null; onRegionSelect: (region: Region | null) => void };
type Gesture = { pointerId: number; start: Point; offset: Point; selecting: boolean; view: View };

export function ImageViewer({ src, alt, sourceWidth, sourceHeight, region, onRegionSelect }: Props) {
  const viewport = useRef<HTMLDivElement>(null);
  const gesture = useRef<Gesture | null>(null);
  const [size, setSize] = useState({ width: 0, height: 0 });
  const [zoom, setZoom] = useState(1);
  const [rotation, setRotation] = useState(0);
  const [offset, setOffset] = useState<Point>({ x: 0, y: 0 });
  const [selecting, setSelecting] = useState(false);
  const [draft, setDraft] = useState<Region | null>(null);
  const cancel = () => { gesture.current = null; setDraft(null); };
  const reset = () => { cancel(); setZoom(1); setRotation(0); setOffset({ x: 0, y: 0 }); };
  useLayoutEffect(() => {
    const element = viewport.current;
    if (!element) return;
    const measure = () => { setSize({ width: element.clientWidth, height: element.clientHeight }); gesture.current = null; setDraft(null); };
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, []);
  useLayoutEffect(() => { reset(); setSelecting(false); }, [src]);
  const view: View = { ...size, sourceWidth, sourceHeight, zoom, rotation, offset };
  const fit = Math.min(size.width / sourceWidth, size.height / sourceHeight);
  const point = (event: ReactPointerEvent<HTMLDivElement>): Point => {
    const bounds = event.currentTarget.getBoundingClientRect();
    return { x: event.clientX - bounds.left, y: event.clientY - bounds.top };
  };
  function move(event: ReactPointerEvent<HTMLDivElement>) {
    const active = gesture.current;
    if (!active || active.pointerId !== event.pointerId) return;
    const current = point(event);
    if (active.selecting) setDraft(selectionRegion(active.start, current, active.view));
    else setOffset({ x: active.offset.x + current.x - active.start.x, y: active.offset.y + current.y - active.start.y });
  }
  const overlay = (value: Region, isDraft = false) => <span className={`region-overlay${isDraft ? " draft" : ""}`} style={{ left: `${value.x / sourceWidth * 100}%`, top: `${value.y / sourceHeight * 100}%`, width: `${value.width / sourceWidth * 100}%`, height: `${value.height / sourceHeight * 100}%`, boxSizing: "border-box" }} />;
  return <div className="image-viewer-shell">
    <div className="image-tools">
      <button type="button" aria-label="Zoom in" onClick={() => { cancel(); setZoom((value) => Math.min(4, value + .25)); }}>+</button>
      <button type="button" aria-label="Zoom out" onClick={() => { cancel(); setZoom((value) => Math.max(.5, value - .25)); }}>−</button>
      <button type="button" aria-label="Rotate clockwise" onClick={() => { cancel(); setRotation((value) => (value + 90) % 360); }}>↻</button>
      <button type="button" aria-pressed={selecting} className={selecting ? "tool-active" : ""} onClick={() => { cancel(); setSelecting((value) => !value); }}>Select</button>
      <button type="button" onClick={reset}>Reset</button><span>{zoom.toFixed(2)}× · {rotation}°</span>
    </div>
    <div ref={viewport} className={`image-viewer ${selecting ? "select-mode" : ""}`}
      onPointerDown={(event) => {
        if (event.button !== 0 || gesture.current) return;
        const start = point(event);
        if (selecting && !viewportToSource(start, view)) return;
        event.preventDefault();
        event.currentTarget.setPointerCapture(event.pointerId);
        gesture.current = { pointerId: event.pointerId, start, offset, selecting, view };
        setDraft(null);
      }}
      onPointerMove={move}
      onPointerUp={(event) => {
        const active = gesture.current;
        if (!active || active.pointerId !== event.pointerId) return;
        if (active.selecting) {
          const selected = selectionRegion(active.start, point(event), active.view);
          if (selected) { onRegionSelect(selected); setSelecting(false); }
        } else move(event);
        cancel();
        if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
      }}
      onPointerCancel={cancel} onLostPointerCapture={cancel}>
      <div style={{ position: "absolute", width: sourceWidth * fit, height: sourceHeight * fit, left: (size.width - sourceWidth * fit) / 2, top: (size.height - sourceHeight * fit) / 2, transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom}) rotate(${rotation}deg)`, transformOrigin: "center", pointerEvents: "none" }}>
        <img src={src} alt={alt} draggable={false} style={{ display: "block", width: "100%", height: "100%" }} />
        {region && overlay(region)}{draft && overlay(draft, true)}
      </div>
    </div>
  </div>;
}
