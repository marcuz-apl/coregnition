export type Point = { x: number; y: number };
export type Region = Point & { width: number; height: number };
export type View = { width: number; height: number; sourceWidth: number; sourceHeight: number; zoom: number; rotation: number; offset: Point };
const clamp = (value: number, max: number) => Math.max(0, Math.min(max, value));
export function viewportToSource(point: Point, view: View, clip = false): Point | null {
  const scale = Math.min(view.width / view.sourceWidth, view.height / view.sourceHeight) * view.zoom;
  if (!Number.isFinite(scale) || scale <= 0) return null;
  const angle = view.rotation * Math.PI / 180;
  const dx = point.x - view.width / 2 - view.offset.x;
  const dy = point.y - view.height / 2 - view.offset.y;
  const x = (dx * Math.cos(angle) + dy * Math.sin(angle)) / scale + view.sourceWidth / 2;
  const y = (-dx * Math.sin(angle) + dy * Math.cos(angle)) / scale + view.sourceHeight / 2;
  if (!clip && (x < -1e-8 || y < -1e-8 || x > view.sourceWidth + 1e-8 || y > view.sourceHeight + 1e-8)) return null;
  return { x: clamp(x, view.sourceWidth), y: clamp(y, view.sourceHeight) };
}
export function selectionRegion(start: Point, end: Point, view: View): Region | null {
  const a = viewportToSource(start, view);
  const b = viewportToSource({ x: clamp(end.x, view.width), y: clamp(end.y, view.height) }, view, true);
  if (!a || !b) return null;
  const left = Math.round(Math.min(a.x, b.x)), top = Math.round(Math.min(a.y, b.y));
  const right = Math.round(Math.max(a.x, b.x)), bottom = Math.round(Math.max(a.y, b.y));
  return right > left && bottom > top ? { x: left, y: top, width: right - left, height: bottom - top } : null;
}
