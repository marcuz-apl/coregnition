package io.github.marcuzapl.coregnition.desktop;

record SourceRegion(int x, int y, int width, int height) {
    static SourceRegion fromDrag(double x1, double y1, double x2, double y2, int width, int height) {
        if (!Double.isFinite(x1) || !Double.isFinite(y1) || !Double.isFinite(x2) || !Double.isFinite(y2)
                || x1 == x2 || y1 == y2) throw new IllegalArgumentException("Select a region with positive width and height");
        int left = (int) Math.floor(Math.max(0, Math.min(width, Math.min(x1, x2))));
        int top = (int) Math.floor(Math.max(0, Math.min(height, Math.min(y1, y2))));
        int right = (int) Math.ceil(Math.max(0, Math.min(width, Math.max(x1, x2))));
        int bottom = (int) Math.ceil(Math.max(0, Math.min(height, Math.max(y1, y2))));
        if (right <= left || bottom <= top) throw new IllegalArgumentException("Select a region inside the image");
        return new SourceRegion(left, top, right - left, bottom - top);
    }
}
