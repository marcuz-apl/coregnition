package io.github.marcuzapl.coregnition.desktop;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SourceRegionTest {
    @Test void mapsReverseDragAndClampsToOriginalImage() {
        assertEquals(new SourceRegion(0, 20, 80, 80), SourceRegion.fromDrag(80, 120, -10, 20, 100, 100));
    }
    @Test void includesFractionalEdgePixels() {
        assertEquals(new SourceRegion(1, 2, 3, 4), SourceRegion.fromDrag(1.8, 2.9, 3.1, 5.1, 100, 100));
    }
    @Test void rejectsEmptyRegion() {
        assertThrows(IllegalArgumentException.class, () -> SourceRegion.fromDrag(5, 5, 5, 5, 100, 100));
    }
}
