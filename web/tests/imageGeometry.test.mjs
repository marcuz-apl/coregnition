import test from 'node:test';
import assert from 'node:assert/strict';
import { viewportToSource, selectionRegion } from '../src/imageGeometry.ts';
const view = { width: 200, height: 200, sourceWidth: 400, sourceHeight: 200, zoom: 1, rotation: 0, offset: { x: 0, y: 0 } };
test('letterboxing is rejected and image pixels map to source coordinates', () => {
  assert.equal(viewportToSource({ x: 100, y: 25 }, view), null);
  assert.deepEqual(viewportToSource({ x: 50, y: 75 }, view), { x: 100, y: 50 });
});
test('zoom and pan are inverted before mapping a drag', () => {
  const transformed = { ...view, zoom: 2, offset: { x: 30, y: -20 } };
  assert.deepEqual(selectionRegion({ x: 30, y: 30 }, { x: 130, y: 80 }, transformed), { x: 100, y: 50, width: 100, height: 50 });
});
test('quarter-turn rotation maps a screen rectangle back to source pixels', () => {
  assert.deepEqual(selectionRegion({ x: 125, y: 50 }, { x: 100, y: 100 }, { ...view, rotation: 90 }), { x: 100, y: 50, width: 100, height: 50 });
});
test('reverse dragging keeps the original anchor across successive updates', () => {
  const anchor = { x: 150, y: 125 };
  assert.deepEqual(selectionRegion(anchor, { x: 100, y: 100 }, view), { x: 200, y: 100, width: 100, height: 50 });
  assert.deepEqual(selectionRegion(anchor, { x: 50, y: 75 }, view), { x: 100, y: 50, width: 200, height: 100 });
});
test('endpoints clip at the image and viewport boundaries and outside starts are rejected', () => {
  assert.deepEqual(selectionRegion({ x: 100, y: 100 }, { x: 300, y: 300 }, view), { x: 200, y: 100, width: 200, height: 100 });
  assert.equal(selectionRegion({ x: 100, y: 25 }, { x: 100, y: 100 }, view), null);
  assert.equal(selectionRegion({ x: 100, y: 100 }, { x: 100, y: 100 }, view), null);
});
